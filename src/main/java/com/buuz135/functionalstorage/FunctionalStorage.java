package com.buuz135.functionalstorage;

import com.buuz135.functionalstorage.block.*;
import com.buuz135.functionalstorage.block.tile.*;
import com.buuz135.functionalstorage.client.*;
import com.buuz135.functionalstorage.client.loader.FramedModel;
import com.buuz135.functionalstorage.data.FunctionalStorageBlockTagsProvider;
import com.buuz135.functionalstorage.data.FunctionalStorageBlockstateProvider;
import com.buuz135.functionalstorage.data.FunctionalStorageItemTagsProvider;
import com.buuz135.functionalstorage.data.FunctionalStorageLangProvider;
import com.buuz135.functionalstorage.inventory.BigInventoryHandler;
import com.buuz135.functionalstorage.inventory.item.DrawerStackItemHandler;
import com.buuz135.functionalstorage.item.ConfigurationToolItem;
import com.buuz135.functionalstorage.item.LinkingToolItem;
import com.buuz135.functionalstorage.item.StorageUpgradeItem;
import com.buuz135.functionalstorage.item.UpgradeItem;
import com.buuz135.functionalstorage.network.EnderDrawerSyncMessage;
import com.buuz135.functionalstorage.recipe.DrawerlessWoodIngredient;
import com.buuz135.functionalstorage.recipe.FramedDrawerRecipe;
import com.buuz135.functionalstorage.util.DrawerWoodType;
import com.buuz135.functionalstorage.util.FabricUtil;
import com.buuz135.functionalstorage.util.IWoodType;
import com.hrznstudio.titanium.module.ModuleController;
import com.hrznstudio.titanium.nbthandler.NBTManager;
import com.hrznstudio.titanium.network.NetworkHandler;
import com.hrznstudio.titanium.tab.AdvancedTitaniumTab;
import io.github.fabricators_of_create.porting_lib.crafting.CraftingHelper;
import io.github.fabricators_of_create.porting_lib.event.common.BlockEvents;
import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import io.github.fabricators_of_create.porting_lib.util.client.ClientHooks;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

// The value here should match an entry in the META-INF/mods.toml file
public class FunctionalStorage extends ModuleController implements ModInitializer {

    public final static String MOD_ID = "functionalstorage";
    public static NetworkHandler NETWORK = new NetworkHandler(MOD_ID);

    static {
        NETWORK.registerMessage(EnderDrawerSyncMessage.class);
    }

    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

    public static List<IWoodType> WOOD_TYPES = new ArrayList<>();

    public static HashMap<DrawerType, List<Pair<RegistryObject<Block>, RegistryObject<BlockEntityType<?>>>>> DRAWER_TYPES = new HashMap<>();
    public static Pair<RegistryObject<Block>, RegistryObject<BlockEntityType<?>>> COMPACTING_DRAWER;
    public static Pair<RegistryObject<Block>, RegistryObject<BlockEntityType<?>>> DRAWER_CONTROLLER;
    public static Pair<RegistryObject<Block>, RegistryObject<BlockEntityType<?>>> ARMORY_CABINET;
    public static Pair<RegistryObject<Block>, RegistryObject<BlockEntityType<?>>> ENDER_DRAWER;
    public static Pair<RegistryObject<Block>, RegistryObject<BlockEntityType<?>>> FRAMED_COMPACTING_DRAWER;
    public static Pair<RegistryObject<Block>, RegistryObject<BlockEntityType<?>>> FLUID_DRAWER_1;
    public static Pair<RegistryObject<Block>, RegistryObject<BlockEntityType<?>>> FLUID_DRAWER_2;
    public static Pair<RegistryObject<Block>, RegistryObject<BlockEntityType<?>>> FLUID_DRAWER_4;
    public static Pair<RegistryObject<Block>, RegistryObject<BlockEntityType<?>>> CONTROLLER_EXTENSION;
    public static Pair<RegistryObject<Block>, RegistryObject<BlockEntityType<?>>> SIMPLE_COMPACTING_DRAWER;

    public static RegistryObject<Item> LINKING_TOOL;
    public static HashMap<StorageUpgradeItem.StorageTier, RegistryObject<Item>> STORAGE_UPGRADES = new HashMap<>();
    public static RegistryObject<Item> COLLECTOR_UPGRADE;
    public static RegistryObject<Item> PULLING_UPGRADE;
    public static RegistryObject<Item> PUSHING_UPGRADE;
    public static RegistryObject<Item> VOID_UPGRADE;
    public static RegistryObject<Item> CONFIGURATION_TOOL;
    public static RegistryObject<Item> REDSTONE_UPGRADE;
    public static RegistryObject<Item> CREATIVE_UPGRADE;

    public static AdvancedTitaniumTab TAB = new AdvancedTitaniumTab("functionalstorage", true);

    public FunctionalStorage() {
        super(MOD_ID);
    }

    @Override
    public void onInitialize() {
        ForgeMod.enableMilkFluid();
        ClientHooks.wrapModTooltips(MOD_ID);
        NETWORK.get().initServerListener();
        BlockEvents.BLOCK_BREAK.register(breakEvent -> {
            if (breakEvent.getPlayer().isCreative()) {
                if (breakEvent.getState().getBlock() instanceof DrawerBlock) {
                    int hit = ((DrawerBlock) breakEvent.getState().getBlock()).getHit(breakEvent.getState(), breakEvent.getPlayer().getLevel(), breakEvent.getPos(), breakEvent.getPlayer());
                    if (hit != -1) {
                        breakEvent.setCanceled(true);
                        ((DrawerBlock) breakEvent.getState().getBlock()).attack(breakEvent.getState(), breakEvent.getPlayer().getLevel(), breakEvent.getPos(), breakEvent.getPlayer());
                    }
                }
                if (breakEvent.getState().getBlock() instanceof CompactingDrawerBlock) {
                    int hit = ((CompactingDrawerBlock) breakEvent.getState().getBlock()).getHit(breakEvent.getState(), breakEvent.getPlayer().getLevel(), breakEvent.getPos(), breakEvent.getPlayer());
                    if (hit != -1) {
                        breakEvent.setCanceled(true);
                        ((CompactingDrawerBlock) breakEvent.getState().getBlock()).attack(breakEvent.getState(), breakEvent.getPlayer().getLevel(), breakEvent.getPos(), breakEvent.getPlayer());
                    }
                }
                if (breakEvent.getState().getBlock() instanceof EnderDrawerBlock) {
                    int hit = ((EnderDrawerBlock) breakEvent.getState().getBlock()).getHit(breakEvent.getState(), breakEvent.getPlayer().getLevel(), breakEvent.getPos(), breakEvent.getPlayer());
                    if (hit != -1) {
                        breakEvent.setCanceled(true);
                        ((EnderDrawerBlock) breakEvent.getState().getBlock()).attack(breakEvent.getState(), breakEvent.getPlayer().getLevel(), breakEvent.getPos(), breakEvent.getPlayer());
                    }
                }
                if (breakEvent.getState().getBlock() instanceof FluidDrawerBlock) {
                    int hit = ((FluidDrawerBlock) breakEvent.getState().getBlock()).getHit(breakEvent.getState(), breakEvent.getPlayer().getLevel(), breakEvent.getPos(), breakEvent.getPlayer());
                    if (hit != -1) {
                        breakEvent.setCanceled(true);
                        ((FluidDrawerBlock) breakEvent.getState().getBlock()).attack(breakEvent.getState(), breakEvent.getPlayer().getLevel(), breakEvent.getPos(), breakEvent.getPlayer());
                    }
                }
            }
        });
        CraftingHelper.register(DrawerlessWoodIngredient.NAME, DrawerlessWoodIngredient.SERIALIZER);;
        NBTManager.getInstance().scanTileClassForAnnotations(FramedDrawerTile.class);
        NBTManager.getInstance().scanTileClassForAnnotations(CompactingFramedDrawerTile.class);
        NBTManager.getInstance().scanTileClassForAnnotations(FluidDrawerTile.class);
        NBTManager.getInstance().scanTileClassForAnnotations(SimpleCompactingDrawerTile.class);

        FabricUtil.CONTEXT_ITEM_API_LOOKUP.registerFallback((itemStack, context) -> {
            if (itemStack.getItem() instanceof DrawerBlock.DrawerItem drawerItem) {
                return new DrawerStackItemHandler(context, drawerItem.drawerBlock.getType());
            }
            return null;
        });
    }


    @Override
    protected void initModules() {
        WOOD_TYPES.addAll(List.of(DrawerWoodType.values()));
        for (DrawerType value : DrawerType.values()) {
            for (IWoodType woodType : WOOD_TYPES) {
                var name = woodType.getName() + "_" + value.getSlots();
                if (woodType == DrawerWoodType.FRAMED){
                    var pair = getRegistries().registerBlockWithTileItem(name, () -> new FramedDrawerBlock(value), blockRegistryObject -> () ->
                            new DrawerBlock.DrawerItem((DrawerBlock) blockRegistryObject.get(), new Item.Properties().tab(TAB)));
                    DRAWER_TYPES.computeIfAbsent(value, drawerType -> new ArrayList<>()).add(pair);
                    CompactingFramedDrawerBlock.FRAMED.add(pair.getLeft());
                } else {
                    DRAWER_TYPES.computeIfAbsent(value, drawerType -> new ArrayList<>()).add(getRegistries().registerBlockWithTileItem(name, () -> new DrawerBlock(woodType, value, BlockBehaviour.Properties.copy(woodType.getPlanks())), blockRegistryObject -> () ->
                            new DrawerBlock.DrawerItem((DrawerBlock) blockRegistryObject.get(), new Item.Properties().tab(TAB))));
                }
            }
            DRAWER_TYPES.get(value).forEach(blockRegistryObject -> TAB.addIconStacks(() -> new ItemStack(blockRegistryObject.getLeft().get())));
        }
        COMPACTING_DRAWER = getRegistries().registerBlockWithTile("compacting_drawer", () -> new CompactingDrawerBlock("compacting_drawer", BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        FRAMED_COMPACTING_DRAWER = getRegistries().registerBlockWithTile("compacting_framed_drawer", () -> new CompactingFramedDrawerBlock("compacting_framed_drawer"));
        FLUID_DRAWER_1 = getRegistries().registerBlockWithTile("fluid_1", () -> new FluidDrawerBlock(DrawerType.X_1, BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        FLUID_DRAWER_2 = getRegistries().registerBlockWithTile("fluid_2", () -> new FluidDrawerBlock(DrawerType.X_2, BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        FLUID_DRAWER_4 = getRegistries().registerBlockWithTile("fluid_4", () -> new FluidDrawerBlock(DrawerType.X_4, BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        DRAWER_CONTROLLER = getRegistries().registerBlockWithTile("storage_controller", DrawerControllerBlock::new);
        CONTROLLER_EXTENSION = getRegistries().registerBlockWithTile("controller_extension", ControllerExtensionBlock::new);
        LINKING_TOOL = getRegistries().registerGeneric(Registry.ITEM_REGISTRY, "linking_tool", LinkingToolItem::new);
        for (StorageUpgradeItem.StorageTier value : StorageUpgradeItem.StorageTier.values()) {
            STORAGE_UPGRADES.put(value, getRegistries().registerGeneric(Registry.ITEM_REGISTRY, value.name().toLowerCase(Locale.ROOT) + (value == StorageUpgradeItem.StorageTier.IRON ? "_downgrade" : "_upgrade"), () -> new StorageUpgradeItem(value)));
        }
        SIMPLE_COMPACTING_DRAWER = getRegistries().registerBlockWithTile("simple_compacting_drawer", () -> new SimpleCompactingDrawerBlock("simple_compacting_drawer", BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        COLLECTOR_UPGRADE = getRegistries().registerGeneric(Registry.ITEM_REGISTRY, "collector_upgrade", () -> new UpgradeItem(new Item.Properties(), UpgradeItem.Type.UTILITY));
        PULLING_UPGRADE = getRegistries().registerGeneric(Registry.ITEM_REGISTRY, "puller_upgrade", () -> new UpgradeItem(new Item.Properties(), UpgradeItem.Type.UTILITY));
        PUSHING_UPGRADE = getRegistries().registerGeneric(Registry.ITEM_REGISTRY, "pusher_upgrade", () -> new UpgradeItem(new Item.Properties(), UpgradeItem.Type.UTILITY));
        VOID_UPGRADE = getRegistries().registerGeneric(Registry.ITEM_REGISTRY, "void_upgrade", () -> new UpgradeItem(new Item.Properties(), UpgradeItem.Type.UTILITY));
        ARMORY_CABINET = getRegistries().registerBlockWithTile("armory_cabinet", ArmoryCabinetBlock::new);
        CONFIGURATION_TOOL = getRegistries().registerGeneric(Registry.ITEM_REGISTRY, "configuration_tool", ConfigurationToolItem::new);
        ENDER_DRAWER = getRegistries().registerBlockWithTile("ender_drawer", EnderDrawerBlock::new);
        REDSTONE_UPGRADE = getRegistries().registerGeneric(Registry.ITEM_REGISTRY, "redstone_upgrade", () -> new UpgradeItem(new Item.Properties(), UpgradeItem.Type.UTILITY));
        CREATIVE_UPGRADE = getRegistries().registerGeneric(Registry.ITEM_REGISTRY, "creative_vending_upgrade", () -> new UpgradeItem(new Item.Properties(), UpgradeItem.Type.STORAGE) {
            @Override
            public boolean isFoil(ItemStack p_41453_) {
                return true;
            }
        });
        getRegistries().registerGeneric(Registry.RECIPE_SERIALIZER_REGISTRY, "framed_recipe", () -> FramedDrawerRecipe.SERIALIZER);
    }

    public enum DrawerType {
        X_1(1, 32 * 64, "1x1", integer -> Pair.of(16, 16)),
        X_2(2, 16 * 64, "1x2", integer -> {
            if (integer == 0) return Pair.of(16, 28);
            return Pair.of(16, 4);
        }),
        X_4(4, 8 * 64, "2x2", integer -> {
            if (integer == 0) return Pair.of(28, 28);
            if (integer == 1) return Pair.of(4, 28);
            if (integer == 2) return Pair.of(28, 4);
            return Pair.of(4, 4);
        });

        private final int slots;
        private final int slotAmount;
        private final String displayName;
        private final Function<Integer, Pair<Integer, Integer>> slotPosition;

        private DrawerType(int slots, int slotAmount, String displayName, Function<Integer, Pair<Integer, Integer>> slotPosition) {
            this.slots = slots;
            this.slotAmount = slotAmount;
            this.displayName = displayName;
            this.slotPosition = slotPosition;
        }

        public int getSlots() {
            return slots;
        }

        public int getSlotAmount() {
            return slotAmount;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Function<Integer, Pair<Integer, Integer>> getSlotPosition() {
            return slotPosition;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void onClient() {
        EventManager.mod(EntityRenderersEvent.RegisterRenderers.class).process(registerRenderers -> {
            for (DrawerType value : DrawerType.values()) {
                DRAWER_TYPES.get(value).forEach(blockRegistryObject -> {
                    registerRenderers.registerBlockEntityRenderer((BlockEntityType<? extends DrawerTile>) blockRegistryObject.getRight().get(), p_173571_ -> new DrawerRenderer());
                });
            }
            registerRenderers.registerBlockEntityRenderer((BlockEntityType<? extends CompactingDrawerTile>) COMPACTING_DRAWER.getRight().get(), p_173571_ -> new CompactingDrawerRenderer());
            registerRenderers.registerBlockEntityRenderer((BlockEntityType<? extends CompactingDrawerTile>) FRAMED_COMPACTING_DRAWER.getRight().get(), p_173571_ -> new CompactingDrawerRenderer());
            registerRenderers.registerBlockEntityRenderer((BlockEntityType<? extends DrawerControllerTile>) DRAWER_CONTROLLER.getRight().get(), p -> new ControllerRenderer());
            registerRenderers.registerBlockEntityRenderer((BlockEntityType<? extends EnderDrawerTile>) ENDER_DRAWER.getRight().get(), p_173571_ -> new EnderDrawerRenderer());
            registerRenderers.registerBlockEntityRenderer((BlockEntityType<? extends FluidDrawerTile>) FLUID_DRAWER_1.getRight().get(), p_173571_ -> new FluidDrawerRenderer());
            registerRenderers.registerBlockEntityRenderer((BlockEntityType<? extends FluidDrawerTile>) FLUID_DRAWER_2.getRight().get(), p_173571_ -> new FluidDrawerRenderer());
            registerRenderers.registerBlockEntityRenderer((BlockEntityType<? extends FluidDrawerTile>) FLUID_DRAWER_4.getRight().get(), p_173571_ -> new FluidDrawerRenderer());
            registerRenderers.registerBlockEntityRenderer((BlockEntityType<? extends SimpleCompactingDrawerTile>) SIMPLE_COMPACTING_DRAWER.getRight().get(), p_173571_ -> new SimpleCompactingDrawerRenderer());

        }).subscribe();
        EventManager.mod(RegisterColorHandlersEvent.Item.class).process(item -> {
            item.getItemColors().register((stack, tint) -> {
                CompoundTag tag = stack.getOrCreateTag();
                LinkingToolItem.LinkingMode linkingMode = LinkingToolItem.getLinkingMode(stack);
                LinkingToolItem.ActionMode linkingAction = LinkingToolItem.getActionMode(stack);
                if (tint != 0 && stack.getOrCreateTag().contains(LinkingToolItem.NBT_ENDER)) {
                    return new Color(44, 150, 88).getRGB();
                }
                if (tint == 3 && tag.contains(LinkingToolItem.NBT_CONTROLLER)) {
                    return Color.RED.getRGB();
                }
                if (tint == 1) {
                    return linkingMode.getColor().getValue();
                }
                if (tint == 2) {
                    return linkingAction.getColor().getValue();
                }
                return 0xffffff;
            }, LINKING_TOOL.get());
            item.getItemColors().register((stack, tint) -> {
                ConfigurationToolItem.ConfigurationAction action = ConfigurationToolItem.getAction(stack);
                if (tint == 1) {
                    return action.getColor().getValue();
                }
                return 0xffffff;
            }, CONFIGURATION_TOOL.get());
        }).subscribe();
        EventManager.mod(FMLClientSetupEvent.class).process(event -> {
            for (DrawerType value : DrawerType.values()) {
                for (RegistryObject<Block> blockRegistryObject : DRAWER_TYPES.get(value).stream().map(Pair::getLeft).collect(Collectors.toList())) {
                    ItemBlockRenderTypes.setRenderLayer(blockRegistryObject.get(), RenderType.cutout());
                }
            }
            ItemBlockRenderTypes.setRenderLayer(COMPACTING_DRAWER.getLeft().get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(FRAMED_COMPACTING_DRAWER.getLeft().get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ENDER_DRAWER.getLeft().get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(FLUID_DRAWER_1.getLeft().get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(FLUID_DRAWER_2.getLeft().get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(FLUID_DRAWER_4.getLeft().get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(SIMPLE_COMPACTING_DRAWER.getLeft().get(), RenderType.cutout());
        }).subscribe();
        EventManager.forge(RenderTooltipEvent.Pre.class).process(itemTooltipEvent -> {
            if (itemTooltipEvent.getItemStack().getItem().equals(FunctionalStorage.ENDER_DRAWER.getLeft().get().asItem()) && itemTooltipEvent.getItemStack().hasTag()) {
                TooltipUtil.renderItems(itemTooltipEvent.getPoseStack(), EnderDrawerBlock.getFrequencyDisplay(itemTooltipEvent.getItemStack().getTag().getCompound("Tile").getString("frequency")), itemTooltipEvent.getX() + 14, itemTooltipEvent.getY() + 11);
            }
            if (itemTooltipEvent.getItemStack().is(FunctionalStorage.LINKING_TOOL.get()) && itemTooltipEvent.getItemStack().getOrCreateTag().contains(LinkingToolItem.NBT_ENDER)) {
                TooltipUtil.renderItems(itemTooltipEvent.getPoseStack(), EnderDrawerBlock.getFrequencyDisplay(itemTooltipEvent.getItemStack().getOrCreateTag().getString(LinkingToolItem.NBT_ENDER)), itemTooltipEvent.getX() + 14, itemTooltipEvent.getY() + 11);
            }
            itemTooltipEvent.getItemStack().getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
                if (iItemHandler instanceof DrawerStackItemHandler) {
                    int i = 0;
                    for (BigInventoryHandler.BigStack storedStack : ((DrawerStackItemHandler) iItemHandler).getStoredStacks()) {
                        TooltipUtil.renderItemAdvanced(itemTooltipEvent.getPoseStack(), storedStack.getStack(), itemTooltipEvent.getX() + 20 + 26 * i, itemTooltipEvent.getY() + 11, 512, NumberUtils.getFormatedBigNumber(storedStack.getAmount()) + "/" + NumberUtils.getFormatedBigNumber(iItemHandler.getSlotLimit(i)));
                        ++i;
                    }
                }
            });
        }).subscribe();
        EventManager.mod(ModelEvent.RegisterGeometryLoaders.class).process(modelRegistryEvent -> {
            modelRegistryEvent.register("framedblock", FramedModel.Loader.INSTANCE);
        }).subscribe();
    }

    @Override
    public void addDataProvider(GatherDataEvent event) {
        NonNullLazy<List<Block>> blocksToProcess = NonNullLazy.of(() ->
                ForgeRegistries.BLOCKS.getValues()
                        .stream()
                        .filter(basicBlock -> Optional.ofNullable(ForgeRegistries.BLOCKS.getKey(basicBlock))
                                .map(ResourceLocation::getNamespace)
                                .filter(MOD_ID::equalsIgnoreCase)
                                .isPresent())
                        .collect(Collectors.toList())
        );
        if (true) {
            event.getGenerator().addProvider(true, new BlockItemModelGeneratorProvider(event.getGenerator(), MOD_ID, blocksToProcess));
            event.getGenerator().addProvider(true, new FunctionalStorageBlockstateProvider(event.getGenerator(), event.getExistingFileHelper(), blocksToProcess));
            event.getGenerator().addProvider(true, new TitaniumLootTableProvider(event.getGenerator(), blocksToProcess));

            event.getGenerator().addProvider(true, new FunctionalStorageItemTagsProvider(event.getGenerator(), new BlockTagsProvider(event.getGenerator()), MOD_ID, event.getExistingFileHelper()));
            event.getGenerator().addProvider(true, new FunctionalStorageLangProvider(event.getGenerator(), MOD_ID, "en_us"));
            event.getGenerator().addProvider(true, new FunctionalStorageBlockTagsProvider(event.getGenerator(), MOD_ID, event.getExistingFileHelper()));
            event.getGenerator().addProvider(true, new ItemModelProvider(event.getGenerator(), MOD_ID, event.getExistingFileHelper()) {
                @Override
                protected void registerModels() {
                    blocksToProcess.get().forEach(block -> withExistingParent(ForgeRegistries.BLOCKS.getKey(block).getPath(), new ResourceLocation(FunctionalStorage.MOD_ID, "block/" + ForgeRegistries.BLOCKS.getKey(block).getPath())));
                    for (StorageUpgradeItem.StorageTier storageTier : STORAGE_UPGRADES.keySet()) {
                        item(STORAGE_UPGRADES.get(storageTier).get());
                    }
                    item(COLLECTOR_UPGRADE.get());
                    item(PULLING_UPGRADE.get());
                    item(PUSHING_UPGRADE.get());
                    item(VOID_UPGRADE.get());
                    item(REDSTONE_UPGRADE.get());
                    item(CREATIVE_UPGRADE.get());
                }

                private void item(Item item) {
                    singleTexture(ForgeRegistries.ITEMS.getKey(item).getPath(), new ResourceLocation("minecraft:item/generated"), "layer0", new ResourceLocation(MOD_ID, "items/" + ForgeRegistries.ITEMS.getKey(item).getPath()));
                }
            });
            event.getGenerator().addProvider(true, new BlockModelProvider(event.getGenerator(), MOD_ID, event.getExistingFileHelper()) {
                @Override
                protected void registerModels() {
                    for (DrawerType value : DrawerType.values()) {
                        for (RegistryObject<Block> blockRegistryObject : DRAWER_TYPES.get(value).stream().map(Pair::getLeft).collect(Collectors.toList())) {
                            if (blockRegistryObject.get() instanceof FramedDrawerBlock) {
                                continue;
                            }
                            withExistingParent(ForgeRegistries.BLOCKS.getKey(blockRegistryObject.get()).getPath() + "_locked", modLoc(ForgeRegistries.BLOCKS.getKey(blockRegistryObject.get()).getPath()))
                                    .texture("lock_icon", modLoc("blocks/lock"));
                        }
                    }
                    withExistingParent(ForgeRegistries.BLOCKS.getKey(COMPACTING_DRAWER.getLeft().get()).getPath() + "_locked", modLoc(ForgeRegistries.BLOCKS.getKey(COMPACTING_DRAWER.getLeft().get()).getPath()))
                            .texture("lock_icon", modLoc("blocks/lock"));
                    withExistingParent(ForgeRegistries.BLOCKS.getKey(ENDER_DRAWER.getLeft().get()).getPath() + "_locked", modLoc(ForgeRegistries.BLOCKS.getKey(ENDER_DRAWER.getLeft().get()).getPath()))
                            .texture("lock_icon", modLoc("blocks/lock"));
                    withExistingParent(ForgeRegistries.BLOCKS.getKey(FLUID_DRAWER_1.getLeft().get()).getPath() + "_locked", modLoc(ForgeRegistries.BLOCKS.getKey(FLUID_DRAWER_1.getLeft().get()).getPath()))
                            .texture("lock_icon", modLoc("blocks/lock"));
                    withExistingParent(ForgeRegistries.BLOCKS.getKey(FLUID_DRAWER_2.getLeft().get()).getPath() + "_locked", modLoc(ForgeRegistries.BLOCKS.getKey(FLUID_DRAWER_2.getLeft().get()).getPath()))
                            .texture("lock_icon", modLoc("blocks/lock"));
                    withExistingParent(ForgeRegistries.BLOCKS.getKey(FLUID_DRAWER_4.getLeft().get()).getPath() + "_locked", modLoc(ForgeRegistries.BLOCKS.getKey(FLUID_DRAWER_4.getLeft().get()).getPath()))
                            .texture("lock_icon", modLoc("blocks/lock"));
                    withExistingParent(ForgeRegistries.BLOCKS.getKey(SIMPLE_COMPACTING_DRAWER.getLeft().get()).getPath() + "_locked", modLoc(ForgeRegistries.BLOCKS.getKey(SIMPLE_COMPACTING_DRAWER.getLeft().get()).getPath()))
                            .texture("lock_icon", modLoc("blocks/lock"));
//                    withExistingParent(ForgeRegistries.BLOCKS.getKey(FRAMED_COMPACTING_DRAWER.getLeft().get()).getPath() + "_locked", modLoc(ForgeRegistries.BLOCKS.getKey(FRAMED_COMPACTING_DRAWER.getLeft().get()).getPath()))
//                            .texture("lock_icon", modLoc("blocks/lock"));
                }
            });
        }
        event.getGenerator().addProvider(true, new TitaniumRecipeProvider(event.getGenerator()) {
            @Override
            public void register(Consumer<FinishedRecipe> consumer) {
                blocksToProcess.get().stream().map(block -> (BasicBlock) block).forEach(basicBlock -> basicBlock.registerRecipe(consumer));
                TitaniumShapedRecipeBuilder.shapedRecipe(STORAGE_UPGRADES.get(StorageUpgradeItem.StorageTier.IRON).get())
                        .pattern("III").pattern("IDI").pattern("III")
                        .define('I', Tags.Items.INGOTS_IRON)
                        .define('D', StorageTags.DRAWER)
                        .save(consumer);
                TitaniumShapedRecipeBuilder.shapedRecipe(VOID_UPGRADE.get())
                        .pattern("III").pattern("IDI").pattern("III")
                        .define('I', Tags.Items.OBSIDIAN)
                        .define('D', StorageTags.DRAWER)
                        .save(consumer);
                TitaniumShapedRecipeBuilder.shapedRecipe(CONFIGURATION_TOOL.get())
                        .pattern("PPG").pattern("PDG").pattern("PEP")
                        .define('P', Items.PAPER)
                        .define('G', Tags.Items.INGOTS_GOLD)
                        .define('D', StorageTags.DRAWER)
                        .define('E', Items.EMERALD)
                        .save(consumer);
                TitaniumShapedRecipeBuilder.shapedRecipe(LINKING_TOOL.get())
                        .pattern("PPG").pattern("PDG").pattern("PEP")
                        .define('P', Items.PAPER)
                        .define('G', Tags.Items.INGOTS_GOLD)
                        .define('D', StorageTags.DRAWER)
                        .define('E', Items.DIAMOND)
                        .save(consumer);
                TitaniumShapedRecipeBuilder.shapedRecipe(STORAGE_UPGRADES.get(StorageUpgradeItem.StorageTier.COPPER).get())
                        .pattern("IBI").pattern("CDC").pattern("IBI")
                        .define('I', Items.COPPER_INGOT)
                        .define('B', Items.COPPER_BLOCK)
                        .define('C', Tags.Items.CHESTS_WOODEN)
                        .define('D', StorageTags.DRAWER)
                        .save(consumer);
                TitaniumShapedRecipeBuilder.shapedRecipe(STORAGE_UPGRADES.get(StorageUpgradeItem.StorageTier.GOLD).get())
                        .pattern("IBI").pattern("CDC").pattern("BIB")
                        .define('I', Tags.Items.INGOTS_GOLD)
                        .define('B', Tags.Items.STORAGE_BLOCKS_GOLD)
                        .define('C', Tags.Items.CHESTS_WOODEN)
                        .define('D', STORAGE_UPGRADES.get(StorageUpgradeItem.StorageTier.COPPER).get())
                        .save(consumer);
                TitaniumShapedRecipeBuilder.shapedRecipe(STORAGE_UPGRADES.get(StorageUpgradeItem.StorageTier.DIAMOND).get())
                        .pattern("IBI").pattern("CDC").pattern("IBI")
                        .define('I', Tags.Items.GEMS_DIAMOND)
                        .define('B', Tags.Items.STORAGE_BLOCKS_DIAMOND)
                        .define('C', Tags.Items.CHESTS_WOODEN)
                        .define('D', STORAGE_UPGRADES.get(StorageUpgradeItem.StorageTier.GOLD).get())
                        .save(consumer);
                TitaniumShapedRecipeBuilder.shapedRecipe(REDSTONE_UPGRADE.get())
                        .pattern("IBI").pattern("CDC").pattern("IBI")
                        .define('I', Items.REDSTONE)
                        .define('B', Items.REDSTONE_BLOCK)
                        .define('C', Items.COMPARATOR)
                        .define('D', StorageTags.DRAWER)
                        .save(consumer);
                UpgradeRecipeBuilder.smithing(Ingredient.of(STORAGE_UPGRADES.get(StorageUpgradeItem.StorageTier.DIAMOND).get()), Ingredient.of(Items.NETHERITE_INGOT), STORAGE_UPGRADES.get(StorageUpgradeItem.StorageTier.NETHERITE).get())
                        .unlocks("has_netherite_ingot", has(Items.NETHERITE_INGOT))
                        .save(consumer, ForgeRegistries.ITEMS.getKey(STORAGE_UPGRADES.get(StorageUpgradeItem.StorageTier.NETHERITE).get()));
                TitaniumShapedRecipeBuilder.shapedRecipe(ARMORY_CABINET.getLeft().get())
                        .pattern("ICI").pattern("CDC").pattern("IBI")
                        .define('I', Tags.Items.STONE)
                        .define('B', Tags.Items.INGOTS_NETHERITE)
                        .define('C', StorageTags.DRAWER)
                        .define('D', Items.COMPARATOR)
                        .save(consumer);
                TitaniumShapedRecipeBuilder.shapedRecipe(PULLING_UPGRADE.get())
                        .pattern("ICI").pattern("IDI").pattern("IBI")
                        .define('I', Tags.Items.STONE)
                        .define('B', Tags.Items.DUSTS_REDSTONE)
                        .define('C', Items.HOPPER)
                        .define('D', StorageTags.DRAWER)
                        .save(consumer);
                TitaniumShapedRecipeBuilder.shapedRecipe(PUSHING_UPGRADE.get())
                        .pattern("IBI").pattern("IDI").pattern("IRI")
                        .define('I', Tags.Items.STONE)
                        .define('B', Tags.Items.DUSTS_REDSTONE)
                        .define('R', Items.HOPPER)
                        .define('D', StorageTags.DRAWER)
                        .save(consumer);
                TitaniumShapedRecipeBuilder.shapedRecipe(COLLECTOR_UPGRADE.get())
                        .pattern("IBI").pattern("RDR").pattern("IBI")
                        .define('I', Tags.Items.STONE)
                        .define('B', Items.HOPPER)
                        .define('R', Tags.Items.DUSTS_REDSTONE)
                        .define('D', StorageTags.DRAWER)
                        .save(consumer);
                TitaniumShapedRecipeBuilder.shapedRecipe(ENDER_DRAWER.getLeft().get())
                        .pattern("PPP").pattern("LCL").pattern("PPP")
                        .define('P', ItemTags.PLANKS)
                        .define('C', Tags.Items.CHESTS_ENDER)
                        .define('L', StorageTags.DRAWER)
                        .save(consumer);
            }
        });
    }
}

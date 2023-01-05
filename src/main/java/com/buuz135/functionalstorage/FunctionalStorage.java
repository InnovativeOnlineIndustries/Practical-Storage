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
import io.github.tropheusj.milk.Milk;
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
        Milk.enableMilkFluid();
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
            } else if (itemStack.getItem() instanceof FluidDrawerBlock.DrawerItem drawerItem) {
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
}

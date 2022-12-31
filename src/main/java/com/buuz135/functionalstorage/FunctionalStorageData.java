package com.buuz135.functionalstorage;

import com.buuz135.functionalstorage.block.FramedDrawerBlock;
import com.buuz135.functionalstorage.data.FunctionalStorageBlockTagsProvider;
import com.buuz135.functionalstorage.data.FunctionalStorageBlockstateProvider;
import com.buuz135.functionalstorage.data.FunctionalStorageItemTagsProvider;
import com.buuz135.functionalstorage.data.FunctionalStorageLangProvider;
import com.buuz135.functionalstorage.item.StorageUpgradeItem;
import com.buuz135.functionalstorage.util.StorageTags;
import com.hrznstudio.titanium.block.BasicBlock;
import com.hrznstudio.titanium.datagenerator.loot.TitaniumLootTableProvider;
import com.hrznstudio.titanium.datagenerator.model.BlockItemModelGeneratorProvider;
import com.hrznstudio.titanium.fabric.NonNullLazy;
import com.hrznstudio.titanium.recipe.generator.TitaniumRecipeProvider;
import com.hrznstudio.titanium.recipe.generator.TitaniumShapedRecipeBuilder;
import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import me.alphamode.forgetags.Tags;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.Registry;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.UpgradeRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockModelProvider;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.buuz135.functionalstorage.FunctionalStorage.*;

public class FunctionalStorageData implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        ExistingFileHelper helper = null;
        NonNullLazy<List<Block>> blocksToProcess = NonNullLazy.of(() ->
                Registry.BLOCK
                        .stream()
                        .filter(basicBlock -> Optional.ofNullable(Registry.BLOCK.getKey(basicBlock))
                                .map(ResourceLocation::getNamespace)
                                .filter(MOD_ID::equalsIgnoreCase)
                                .isPresent())
                        .collect(Collectors.toList())
        );
        if (true) {
            generator.addProvider(true, new BlockItemModelGeneratorProvider(generator, MOD_ID, blocksToProcess));
            generator.addProvider(true, new FunctionalStorageBlockstateProvider(generator, helper, blocksToProcess));
            generator.addProvider(true, new TitaniumLootTableProvider(generator, blocksToProcess));

            generator.addProvider(true, new FunctionalStorageItemTagsProvider(generator));
            generator.addProvider(true, new FunctionalStorageLangProvider(generator, MOD_ID, "en_us"));
            generator.addProvider(true, new FunctionalStorageBlockTagsProvider(generator));
            generator.addProvider(true, new ItemModelProvider(generator, MOD_ID, helper) {
                @Override
                protected void registerModels() {
                    blocksToProcess.get().forEach(block -> withExistingParent(Registry.BLOCK.getKey(block).getPath(), new ResourceLocation(FunctionalStorage.MOD_ID, "block/" + Registry.BLOCK.getKey(block).getPath())));
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
                    singleTexture(Registry.ITEM.getKey(item).getPath(), new ResourceLocation("minecraft:item/generated"), "layer0", new ResourceLocation(MOD_ID, "items/" + Registry.ITEM.getKey(item).getPath()));
                }
            });
            generator.addProvider(true, new BlockModelProvider(generator, MOD_ID, helper) {
                @Override
                protected void registerModels() {
                    for (FunctionalStorage.DrawerType value : FunctionalStorage.DrawerType.values()) {
                        for (RegistryObject<Block> blockRegistryObject : DRAWER_TYPES.get(value).stream().map(Pair::getLeft).collect(Collectors.toList())) {
                            if (blockRegistryObject.get() instanceof FramedDrawerBlock) {
                                continue;
                            }
                            withExistingParent(Registry.BLOCK.getKey(blockRegistryObject.get()).getPath() + "_locked", modLoc(Registry.BLOCK.getKey(blockRegistryObject.get()).getPath()))
                                    .texture("lock_icon", modLoc("blocks/lock"));
                        }
                    }
                    withExistingParent(Registry.BLOCK.getKey(COMPACTING_DRAWER.getLeft().get()).getPath() + "_locked", modLoc(Registry.BLOCK.getKey(COMPACTING_DRAWER.getLeft().get()).getPath()))
                            .texture("lock_icon", modLoc("blocks/lock"));
                    withExistingParent(Registry.BLOCK.getKey(ENDER_DRAWER.getLeft().get()).getPath() + "_locked", modLoc(Registry.BLOCK.getKey(ENDER_DRAWER.getLeft().get()).getPath()))
                            .texture("lock_icon", modLoc("blocks/lock"));
//                    withExistingParent(Registry.BLOCK.getKey(FRAMED_COMPACTING_DRAWER.getLeft().get()).getPath() + "_locked", modLoc(Registry.BLOCK.getKey(FRAMED_COMPACTING_DRAWER.getLeft().get()).getPath()))
//                            .texture("lock_icon", modLoc("blocks/lock"));
                }
            });
        }
        generator.addProvider(true, new TitaniumRecipeProvider(generator) {
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
                        .save(consumer, Registry.ITEM.getKey(STORAGE_UPGRADES.get(StorageUpgradeItem.StorageTier.NETHERITE).get()));
                TitaniumShapedRecipeBuilder.shapedRecipe(DRAWER_CONTROLLER.getLeft().get())
                        .pattern("IBI").pattern("CDC").pattern("IBI")
                        .define('I', Tags.Items.STONE)
                        .define('B', Tags.Items.STORAGE_BLOCKS_QUARTZ)
                        .define('C', StorageTags.DRAWER)
                        .define('D', Items.COMPARATOR)
                        .save(consumer);
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

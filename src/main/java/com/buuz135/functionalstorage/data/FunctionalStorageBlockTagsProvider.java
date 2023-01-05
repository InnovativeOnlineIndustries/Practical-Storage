package com.buuz135.functionalstorage.data;

import com.buuz135.functionalstorage.FunctionalStorage;
import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.tuple.Pair;

import java.util.stream.Collectors;

public class FunctionalStorageBlockTagsProvider extends FabricTagProvider.BlockTagProvider {

    public FunctionalStorageBlockTagsProvider(FabricDataGenerator p_126530_) {
        super(p_126530_);
    }

    @Override
    protected void generateTags() {
        TagAppender<Block> tTagAppender = this.tag(BlockTags.MINEABLE_WITH_AXE);
        for (FunctionalStorage.DrawerType drawerType : FunctionalStorage.DRAWER_TYPES.keySet()) {
            for (RegistryObject<Block> blockRegistryObject : FunctionalStorage.DRAWER_TYPES.get(drawerType).stream().map(Pair::getLeft).collect(Collectors.toList())) {
                tTagAppender.add(blockRegistryObject.get());
            }
        }
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(FunctionalStorage.COMPACTING_DRAWER.getLeft().get())
                .add(FunctionalStorage.DRAWER_CONTROLLER.getLeft().get())
                .add(FunctionalStorage.ARMORY_CABINET.getLeft().get())
                .add(FunctionalStorage.ENDER_DRAWER.getLeft().get())
                .add(FunctionalStorage.FRAMED_COMPACTING_DRAWER.getLeft().get())
                .add(FunctionalStorage.FLUID_DRAWER_1.getLeft().get())
                .add(FunctionalStorage.FLUID_DRAWER_2.getLeft().get())
                .add(FunctionalStorage.FLUID_DRAWER_4.getLeft().get())
                .add(FunctionalStorage.CONTROLLER_EXTENSION.getLeft().get())
                .add(FunctionalStorage.SIMPLE_COMPACTING_DRAWER.getLeft().get())
        ;
    }
}

package com.buuz135.functionalstorage.util;

import com.buuz135.functionalstorage.FunctionalStorage;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class StorageTags {

    public static TagKey<Item> DRAWER = TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation(FunctionalStorage.MOD_ID, "drawer"));
    public static TagKey<Item> IGNORE_CRAFTING_CHECK = TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation(FunctionalStorage.MOD_ID, "ignore_crafting_check"));

}

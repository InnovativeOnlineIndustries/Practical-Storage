package com.buuz135.functionalstorage.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hrznstudio.titanium.util.TagUtil;
import io.github.tropheusj.serialization_hooks.ingredient.IngredientDeserializer;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.buuz135.functionalstorage.FunctionalStorage.MOD_ID;

public class DrawerlessWoodIngredient extends Ingredient {

    public static WoodlessIngredientSerializer SERIALIZER = new WoodlessIngredientSerializer();
    public static ResourceLocation NAME = new ResourceLocation(MOD_ID, "woodless");

    private List<Item> woodless;

    public DrawerlessWoodIngredient() {
        super(Stream.empty());
    }

    @Override
    public ItemStack[] getItems() {
        return getWoods().stream().map(ItemStack::new).toArray(ItemStack[]::new);
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        return getWoods().contains(stack.getItem());
    }

    private List<Item> getWoods(){
        if (woodless == null){
            woodless = TagUtil.getAllEntries(Registry.ITEM, ItemTags.PLANKS).stream().filter(item -> !Registry.ITEM.getKey(item).getNamespace().equalsIgnoreCase("minecraft")).collect(Collectors.toList());
            if (woodless.isEmpty()){
                woodless.add(Items.OAK_PLANKS);
            }
        }
        return woodless;
    }

    @Override
    public JsonElement toJson() {
        JsonObject element = new JsonObject();
        element.addProperty("type", NAME.toString());
        return element;
    }

    public static class WoodlessIngredientSerializer implements IngredientDeserializer {

        @Override
        public Ingredient fromNetwork(FriendlyByteBuf buffer) {
            return new DrawerlessWoodIngredient();
        }

        @Override
        public Ingredient fromJson(JsonObject json) {
            return new DrawerlessWoodIngredient();
        }
    }
}

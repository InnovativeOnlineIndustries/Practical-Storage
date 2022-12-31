package com.buuz135.functionalstorage;

import com.buuz135.functionalstorage.block.EnderDrawerBlock;
import com.buuz135.functionalstorage.block.tile.CompactingDrawerTile;
import com.buuz135.functionalstorage.block.tile.DrawerControllerTile;
import com.buuz135.functionalstorage.block.tile.DrawerTile;
import com.buuz135.functionalstorage.block.tile.EnderDrawerTile;
import com.buuz135.functionalstorage.client.*;
import com.buuz135.functionalstorage.client.loader.FramedModel;
import com.buuz135.functionalstorage.inventory.BigInventoryHandler;
import com.buuz135.functionalstorage.inventory.item.DrawerStackItemHandler;
import com.buuz135.functionalstorage.item.ConfigurationToolItem;
import com.buuz135.functionalstorage.item.LinkingToolItem;
import com.buuz135.functionalstorage.util.FabricUtil;
import com.buuz135.functionalstorage.util.NumberUtils;
import com.buuz135.functionalstorage.util.TooltipUtil;
import io.github.fabricators_of_create.porting_lib.event.client.PreRenderTooltipCallback;
import io.github.fabricators_of_create.porting_lib.event.client.RegisterGeometryLoadersCallback;
import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.util.stream.Collectors;

import static com.buuz135.functionalstorage.FunctionalStorage.*;

public class FunctionalStorageClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        for (FunctionalStorage.DrawerType value : FunctionalStorage.DrawerType.values()) {
            DRAWER_TYPES.get(value).forEach(blockRegistryObject -> {
                BlockEntityRenderers.register((BlockEntityType<? extends DrawerTile>) blockRegistryObject.getRight().get(), p_173571_ -> new DrawerRenderer());
            });
        }
        BlockEntityRenderers.register((BlockEntityType<? extends CompactingDrawerTile>) COMPACTING_DRAWER.getRight().get(), p_173571_ -> new CompactingDrawerRenderer());
        BlockEntityRenderers.register((BlockEntityType<? extends CompactingDrawerTile>) FRAMED_COMPACTING_DRAWER.getRight().get(), p_173571_ -> new CompactingDrawerRenderer());
        BlockEntityRenderers.register((BlockEntityType<? extends DrawerControllerTile>) DRAWER_CONTROLLER.getRight().get(), p -> new ControllerRenderer());
        BlockEntityRenderers.register((BlockEntityType<? extends EnderDrawerTile>) ENDER_DRAWER.getRight().get(), p_173571_ -> new EnderDrawerRenderer());
        ColorProviderRegistry.ITEM.register((stack, tint) -> {
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
        ColorProviderRegistry.ITEM.register((stack, tint) -> {
            ConfigurationToolItem.ConfigurationAction action = ConfigurationToolItem.getAction(stack);
            if (tint == 1) {
                return action.getColor().getValue();
            }
            return 0xffffff;
        }, CONFIGURATION_TOOL.get());
        for (FunctionalStorage.DrawerType value : FunctionalStorage.DrawerType.values()) {
            for (RegistryObject<Block> blockRegistryObject : DRAWER_TYPES.get(value).stream().map(Pair::getLeft).collect(Collectors.toList())) {
                BlockRenderLayerMap.INSTANCE.putBlock(blockRegistryObject.get(), RenderType.cutout());
            }
        }
        BlockRenderLayerMap.INSTANCE.putBlock(COMPACTING_DRAWER.getLeft().get(), RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(FRAMED_COMPACTING_DRAWER.getLeft().get(), RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ENDER_DRAWER.getLeft().get(), RenderType.cutout());
        PreRenderTooltipCallback.EVENT.register((itemStack, poseStack, x, y, screenWidth, screenHeight, font, components) -> {
            if (itemStack.getItem().equals(FunctionalStorage.ENDER_DRAWER.getLeft().get().asItem()) && itemStack.hasTag()) {
                TooltipUtil.renderItems(poseStack, EnderDrawerBlock.getFrequencyDisplay(itemStack.getTag().getCompound("Tile").getString("frequency")), x + 14, y + 11);
            }
            if (itemStack.is(FunctionalStorage.LINKING_TOOL.get()) && itemStack.getOrCreateTag().contains(LinkingToolItem.NBT_ENDER)) {
                TooltipUtil.renderItems(poseStack, EnderDrawerBlock.getFrequencyDisplay(itemStack.getOrCreateTag().getString(LinkingToolItem.NBT_ENDER)), x + 14, y + 11);
            }
            Storage<ItemVariant> iItemHandler = FabricUtil.CONTEXT_ITEM_API_LOOKUP.find(itemStack, ContainerItemContext.withInitial(itemStack));
            if (iItemHandler != null) {
                if (iItemHandler instanceof DrawerStackItemHandler drawerStackItemHandler) {
                    int i = 0;
                    for (BigInventoryHandler.BigStack storedStack : drawerStackItemHandler.getStoredStacks()) {
                        TooltipUtil.renderItemAdvanced(poseStack, storedStack.getStack(), x + 20 + 26 * i, y + 11, 512, NumberUtils.getFormatedBigNumber(storedStack.getAmount()) + "/" + NumberUtils.getFormatedBigNumber(drawerStackItemHandler.getSlotLimit(i)));
                        ++i;
                    }
                }
            }
            return false;
        });
        RegisterGeometryLoadersCallback.EVENT.register(loaders -> {
            loaders.put(new ResourceLocation(MOD_ID, "framedblock"), FramedModel.Loader.INSTANCE);
        });
        NETWORK.get().initClientListener();
        FramedColors.init();
    }
}

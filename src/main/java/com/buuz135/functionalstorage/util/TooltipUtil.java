package com.buuz135.functionalstorage.util;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class TooltipUtil {


    public static void renderItems(PoseStack stack, List<ItemStack> items, int x, int y) {
        for (int i = 0; i < items.size(); i++) {
            renderItemIntoGUI(stack, items.get(i), x + 18 * i, y, 512);
        }
    }

    public static void renderItemIntoGUI(PoseStack matrixStack, ItemStack stack, int x, int y, int z) {
        renderItemModelIntoGUI(matrixStack, stack, x, y, z, Minecraft.getInstance().getItemRenderer().getModel(stack, (Level) null, (LivingEntity) null, 0));
    }

    public static void renderItemAdvanced(PoseStack matrixStack, ItemStack stack, int x, int y, int z, String amount) {
        renderItemModelIntoGUI(matrixStack, stack, x, y, z, Minecraft.getInstance().getItemRenderer().getModel(stack, (Level) null, (LivingEntity) null, 0));
        renderItemStackOverlay(matrixStack, Minecraft.getInstance().font, stack, x, y, amount, amount.length() - 2);
    }

    public static void renderItemStackOverlay(PoseStack matrixStack, Font fr, ItemStack stack, int xPosition, int yPosition, @Nullable String text, int scaled) {
        matrixStack.pushPose();
        matrixStack.translate(0, 0, 512 + 32);
        if (!stack.isEmpty()) {
            if (stack.getCount() != 1 || text != null) {
                String s = text == null ? String.valueOf(stack.getCount()) : text;
                if (text == null && stack.getCount() < 1) {
                    ChatFormatting var10000 = ChatFormatting.RED;
                    s = var10000 + String.valueOf(stack.getCount());
                }

                matrixStack.translate(0.0D, 0.0D, (double) (Minecraft.getInstance().getItemRenderer().blitOffset + 200.0F));
                RenderSystem.disableDepthTest();
                RenderSystem.disableBlend();
                if (scaled >= 2) {
                    matrixStack.pushPose();
                    matrixStack.scale(0.5F, 0.5F, 0.5F);
                    fr.drawShadow(matrixStack, s, (float) ((xPosition + 19 - 2) * 2 - 1 - fr.width(s)), (float) (yPosition * 2 + 24), 16777215);
                    matrixStack.popPose();
                } else if (scaled == 1) {
                    matrixStack.pushPose();
                    matrixStack.scale(0.75F, 0.75F, 0.75F);
                    fr.drawShadow(matrixStack, s, (float) (xPosition - 2) * 1.34F + 24.0F - (float) fr.width(s), (float) yPosition * 1.34F + 14.0F, 16777215);
                    matrixStack.popPose();
                } else {
                    fr.drawShadow(matrixStack, s, (float) (xPosition + 19 - 2 - fr.width(s)), (float) (yPosition + 6 + 3), 16777215);
                }

                RenderSystem.enableDepthTest();
                RenderSystem.enableBlend();
            }
        }
        matrixStack.popPose();
    }

    @Environment(EnvType.CLIENT)
    public static void renderItemModelIntoGUI(PoseStack posestack, ItemStack stack, int x, int y, int z, BakedModel bakedmodel) {
        Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).setFilter(false, false);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        posestack.pushPose();
        posestack.translate((double) x, (double) y, (double) z);
        posestack.translate(8.0D, 8.0D, 0.0D);
        posestack.scale(1.0F, -1.0F, 1.0F);
        posestack.scale(16.0F, 16.0F, 16.0F);
        RenderSystem.applyModelViewMatrix();
        MultiBufferSource.BufferSource multibuffersource$buffersource = Minecraft.getInstance().renderBuffers().bufferSource();
        boolean flag = !bakedmodel.usesBlockLight();
        if (flag) {
            Lighting.setupForFlatItems();
        }

        Minecraft.getInstance().getItemRenderer().render(stack, ItemTransforms.TransformType.GUI, false, posestack, multibuffersource$buffersource, 15728880, OverlayTexture.NO_OVERLAY, bakedmodel);
        multibuffersource$buffersource.endBatch();
        RenderSystem.enableDepthTest();
        if (flag) {
            Lighting.setupFor3DItems();
        }

        posestack.popPose();
        RenderSystem.applyModelViewMatrix();
    }


}

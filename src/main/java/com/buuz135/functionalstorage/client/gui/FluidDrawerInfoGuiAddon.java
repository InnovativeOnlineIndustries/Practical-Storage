package com.buuz135.functionalstorage.client.gui;

import com.buuz135.functionalstorage.fluid.BigFluidHandler;
import com.buuz135.functionalstorage.util.NumberUtils;
import com.hrznstudio.titanium.client.screen.addon.BasicScreenAddon;
import com.hrznstudio.titanium.client.screen.asset.IAssetProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.fabricators_of_create.porting_lib.util.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class FluidDrawerInfoGuiAddon extends BasicScreenAddon {

    private final ResourceLocation gui;
    private final int slotAmount;
    private final Function<Integer, Pair<Integer, Integer>> slotPosition;
    private final Supplier<BigFluidHandler> fluidHandlerSupplier;
    private final Function<Integer, Long> slotMaxAmount;

    public FluidDrawerInfoGuiAddon(int posX, int posY, ResourceLocation gui, int slotAmount, Function<Integer, Pair<Integer, Integer>> slotPosition, Supplier<BigFluidHandler> fluidHandlerSupplier, Function<Integer, Long> slotMaxAmount) {
        super(posX, posY);
        this.gui = gui;
        this.slotAmount = slotAmount;
        this.slotPosition = slotPosition;
        this.fluidHandlerSupplier = fluidHandlerSupplier;
        this.slotMaxAmount = slotMaxAmount;
    }

    public static Rect2i getSizeForSlots(int currentSlot, int slotAmount) {
        if (slotAmount == 1) {
            return new Rect2i(9, 9, 30, 30);
        }
        if (slotAmount == 2) {
            if (currentSlot == 0) return new Rect2i(0, 30, 48, 13);
            if (currentSlot == 1) return new Rect2i(0, 6, 48, 13);
        }
        if (slotAmount == 4) {
            if (currentSlot == 0) return new Rect2i(30, 30, 16, 16);
            if (currentSlot == 1) return new Rect2i(2, 30, 16, 16);
            if (currentSlot == 2) return new Rect2i(30, 2, 16, 16);
            if (currentSlot == 3) return new Rect2i(2, 2, 16, 16);
        }
        return new Rect2i(0, 0, 0, 0);
    }

    public static Rect2i getSizeForHoverSlots(int currentSlot, int slotAmount) {
        if (slotAmount == 1) {
            return new Rect2i(9, 9, 30, 30);
        }
        if (slotAmount == 2) {
            if (currentSlot == 0) return new Rect2i(6, 30, 36, 12);
            if (currentSlot == 1) return new Rect2i(6, 6, 36, 12);
        }
        if (slotAmount == 4) {
            if (currentSlot == 0) return new Rect2i(30, 30, 12, 12);
            if (currentSlot == 1) return new Rect2i(6, 30, 12, 12);
            if (currentSlot == 2) return new Rect2i(30, 6, 12, 12);
            if (currentSlot == 3) return new Rect2i(6, 6, 12, 12);
        }
        return new Rect2i(0, 0, 0, 0);
    }

    @Override
    public int getXSize() {
        return 0;
    }

    @Override
    public int getYSize() {
        return 0;
    }

    @Override
    public void drawBackgroundLayer(PoseStack stack, Screen screen, IAssetProvider provider, int guiX, int guiY, int mouseX, int mouseY, float partialTicks) {
        for (var i = 0; i < slotAmount; i++) {
            var fluidStack = fluidHandlerSupplier.get().getFluidInTank(i);
            if (fluidStack.isEmpty() && fluidHandlerSupplier.get().isDrawerLocked()) {
                fluidStack = fluidHandlerSupplier.get().getFilterStack()[i];
            }
            if (!fluidStack.isEmpty()) {
                renderFluid(stack, screen, guiX, guiY, fluidStack, i, slotAmount);
            }
        }
        RenderSystem.setShaderTexture(0, gui);
        var size = 16 * 2 + 16;
        Screen.blit(stack, guiX + getPosX(), guiY + getPosY(), 0, 0, size, size, size, size);
        for (var i = 0; i < slotAmount; i++) {
            var fluidStack = fluidHandlerSupplier.get().getFluidInTank(i);
            if (!fluidStack.isEmpty()) {
                var x = guiX + slotPosition.apply(i).getLeft() + getPosX();
                var y = guiY + slotPosition.apply(i).getRight() + getPosY();
                var amount = NumberUtils.getFormatedFluidBigNumber(fluidStack.getAmount()) + "/" + NumberUtils.getFormatedFluidBigNumber(slotMaxAmount.apply(i));
                var scale = 0.5f;
                stack.translate(0, 0, 200);
                stack.scale(scale, scale, scale);
                Minecraft.getInstance().font.drawShadow(stack, amount, (x + 17 - Minecraft.getInstance().font.width(amount) / 2) * (1 / scale), (y + 12) * (1 / scale), 0xFFFFFF);
                stack.scale(1 / scale, 1 / scale, 1 / scale);
                stack.translate(0, 0, -200);
            }
        }
    }

    @Override
    public void drawForegroundLayer(PoseStack stack, Screen screen, IAssetProvider provider, int guiX, int guiY, int mouseX, int mouseY, float partialTicks) {
        for (var i = 0; i < slotAmount; i++) {
            var rect = getSizeForHoverSlots(i, slotAmount);
            var x = rect.getX() + getPosX() + guiX;
            var y = rect.getY() + getPosY() + guiY;
            if (mouseX > x && mouseX < x + rect.getWidth() && mouseY > y && mouseY < y + rect.getHeight()) {
                x = getPosX() + rect.getX();
                y = getPosY() + rect.getY();
                stack.translate(0, 0, 200);
                GuiComponent.fill(stack, x, y, x + rect.getWidth(), y + rect.getHeight(), -2130706433);
                stack.translate(0, 0, -200);
                var componentList = new ArrayList<Component>();
                var over = fluidHandlerSupplier.get().getFluidInTank(i);
                if (over.isEmpty()) {
                    componentList.add(Component.translatable("gui.functionalstorage.fluid").withStyle(ChatFormatting.GOLD).append(Component.literal("Empty").withStyle(ChatFormatting.WHITE)));
                } else {
                    componentList.add(Component.translatable("gui.functionalstorage.fluid").withStyle(ChatFormatting.GOLD).append(over.getDisplayName().copy().withStyle(ChatFormatting.WHITE)));
                    var amount = NumberUtils.getFormatedFluidBigNumber(over.getAmount()) + "/" + NumberUtils.getFormatedFluidBigNumber(slotMaxAmount.apply(i));
                    componentList.add(Component.translatable("gui.functionalstorage.amount").withStyle(ChatFormatting.GOLD).append(Component.literal(amount).withStyle(ChatFormatting.WHITE)));
                }
                componentList.add(Component.translatable("gui.functionalstorage.slot").withStyle(ChatFormatting.GOLD).append(Component.literal(i + "").withStyle(ChatFormatting.WHITE)));
                screen.renderTooltip(stack, componentList, Optional.empty(), mouseX - guiX, mouseY - guiY);
            }
        }
    }

    public void renderFluid(PoseStack stack, Screen screen, int guiX, int guiY, FluidStack fluidStack, int slot, int slotAmount) {
        TextureAtlasSprite sprite = FluidVariantRendering.getSprite(fluidStack.getType());
        if (sprite != null) {
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
            Color color = new Color(FluidVariantRendering.getColor(fluidStack.getType()));
            var rect = getSizeForSlots(slot, slotAmount);
            RenderSystem.setShaderColor(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
            RenderSystem.enableBlend();
            for (int x = 0; x < rect.getWidth(); x += 16) {
                for (int y = 0; y < rect.getHeight(); y += 16) {
                    Screen.blit(stack, this.getPosX() + guiX + rect.getX() + x,
                            this.getPosY() + guiY + rect.getY() + y,
                            0,
                            Math.min(16, rect.getWidth() - x),
                            Math.min(16, rect.getHeight() - y),
                            sprite);
                }
            }

            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }


}

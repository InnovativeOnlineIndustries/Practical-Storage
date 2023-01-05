package com.buuz135.functionalstorage.block.tile;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.client.gui.DrawerInfoGuiAddon;
import com.buuz135.functionalstorage.inventory.CompactingInventoryHandler;
import com.buuz135.functionalstorage.util.CompactingUtil;
import com.hrznstudio.titanium.annotation.Save;
import com.hrznstudio.titanium.block.BasicTileBlock;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlotExposedStorage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SimpleCompactingDrawerTile extends ItemControllableDrawerTile<SimpleCompactingDrawerTile> {

    @Save
    public CompactingInventoryHandler handler;
    private boolean hasCheckedRecipes;

    public SimpleCompactingDrawerTile(BasicTileBlock<SimpleCompactingDrawerTile> base, BlockEntityType<SimpleCompactingDrawerTile> blockEntityType, BlockPos pos, BlockState state) {
        super(base, blockEntityType, pos, state);
        this.handler = new CompactingInventoryHandler(2) {
            @Override
            public void onChange() {
                SimpleCompactingDrawerTile.this.markForUpdate();
            }

            @Override
            public int getMultiplier() {
                return getStorageMultiplier();
            }

            @Override
            public boolean isVoid() {
                return SimpleCompactingDrawerTile.this.isVoid();
            }

            @Override
            public boolean hasDowngrade() {
                return SimpleCompactingDrawerTile.this.hasDowngrade();
            }

            @Override
            public boolean isCreative() {
                return SimpleCompactingDrawerTile.this.isCreative();
            }

            @Override
            public boolean isLocked() {
                return SimpleCompactingDrawerTile.this.isLocked();
            }

        };
        this.hasCheckedRecipes = false;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void initClient() {
        super.initClient();
        addGuiAddonFactory(() -> new DrawerInfoGuiAddon(64, 16,
                new ResourceLocation(FunctionalStorage.MOD_ID, "textures/blocks/simple_compacting_drawer_front.png"),
                2,
                integer -> {
                    if (integer == 0) return Pair.of(16, 28);
                    if (integer == 1) return Pair.of(16, 4);
                    return Pair.of(16, 4);
                },
                integer -> getStorage().getStackInSlot(integer),
                integer -> getStorage().getSlotLimit(integer)
        ));
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state, SimpleCompactingDrawerTile blockEntity) {
        super.serverTick(level, pos, state, blockEntity);
        if (!hasCheckedRecipes) {
            if (!handler.getParent().isEmpty()) {
                CompactingUtil compactingUtil = new CompactingUtil(this.level, 2);
                compactingUtil.setup(handler.getParent());
                handler.setup(compactingUtil);
            }
            hasCheckedRecipes = true;
        }
    }

    public InteractionResult onSlotActivated(Player playerIn, InteractionHand hand, Direction facing, double hitX, double hitY, double hitZ, int slot) {
        ItemStack stack = playerIn.getItemInHand(hand);
        if (stack.getItem().equals(FunctionalStorage.CONFIGURATION_TOOL.get()) || stack.getItem().equals(FunctionalStorage.LINKING_TOOL.get()))
            return InteractionResult.PASS;
        if (!handler.isSetup() && slot != -1) {
            stack = playerIn.getItemInHand(hand).copy();
            stack.setCount(1);
            CompactingUtil compactingUtil = new CompactingUtil(this.level, 2);
            compactingUtil.setup(stack);
            handler.setup(compactingUtil);
            for (int i = 0; i < handler.getResultList().size(); i++) {
                if (ItemStack.isSame(handler.getResultList().get(i).getResult(), stack)) {
                    slot = i;
                    break;
                }
            }
        }
        return super.onSlotActivated(playerIn, hand, facing, hitX, hitY, hitZ, slot);
    }

    @Override
    public int getStorageSlotAmount() {
        return 3;
    }

    @Override
    public SlotExposedStorage getStorage() {
        return handler;
    }

    @Override
    public int getBaseSize(int slot) {
        return handler.getSlotLimitBase(slot);
    }

    @Override
    public Storage<ItemVariant> getItemStorage(Direction side) {
        return handler;
    }

    @NotNull
    @Override
    public SimpleCompactingDrawerTile getSelf() {
        return this;
    }

    public CompactingInventoryHandler getHandler() {
        return handler;
    }

}

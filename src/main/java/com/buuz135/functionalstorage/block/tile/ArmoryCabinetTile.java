package com.buuz135.functionalstorage.block.tile;

import com.buuz135.functionalstorage.inventory.ArmoryCabinetInventoryHandler;
import com.hrznstudio.titanium.annotation.Save;
import com.hrznstudio.titanium.block.BasicTileBlock;
import com.hrznstudio.titanium.block.tile.ActiveTile;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlotExposedStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class ArmoryCabinetTile extends ActiveTile<ArmoryCabinetTile> {

    @Save
    public ArmoryCabinetInventoryHandler handler;

    public ArmoryCabinetTile(BasicTileBlock<ArmoryCabinetTile> base, BlockEntityType<?> entityType, BlockPos pos, BlockState state) {
        super(base, entityType, pos, state);
        this.handler = new ArmoryCabinetInventoryHandler() {
            @Override
            public void onChange() {
                ArmoryCabinetTile.this.markForUpdate();
            }
        };
    }

    @Override
    public Storage<ItemVariant> getItemStorage(Direction side) {
        return handler;
    }

    public SlotExposedStorage getStorage() {
        return handler;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, blockEntity -> new CompoundTag());
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        //super.onDataPacket(net, pkt);
    }

    @Override
    @Nonnull
    public CompoundTag getUpdateTag() {
        CompoundTag compoundTag = new CompoundTag();
        return compoundTag;
    }

    public boolean isEverythingEmpty() {
        for (int i = 0; i < getStorage().getSlots(); i++) {
            if (!getStorage().getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    @Override
    public ArmoryCabinetTile getSelf() {
        return this;
    }
}

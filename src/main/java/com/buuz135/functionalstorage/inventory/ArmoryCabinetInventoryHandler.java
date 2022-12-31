package com.buuz135.functionalstorage.inventory;

import com.buuz135.functionalstorage.block.config.FunctionalStorageConfig;
import io.github.fabricators_of_create.porting_lib.extensions.INBTSerializable;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlotExposedStorage;
import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class ArmoryCabinetInventoryHandler extends SnapshotParticipant<List<ItemStack>> implements SlotExposedStorage, INBTSerializable<CompoundTag> {

    public List<ItemStack> stackList;

    public ArmoryCabinetInventoryHandler() {
        this.stackList = create();
    }

    @Override
    public int getSlots() {
        return FunctionalStorageConfig.ARMORY_CABINET_SIZE;
    }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < this.stackList.size()){
            return this.stackList.get(slot);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        updateSnapshots(transaction);
        for (int i = 0; i < this.stackList.size(); i++) {
            if (isValid(i, resource, maxAmount)) {
                // Todo: handle overflow with stacks over 64
                this.stackList.set(i, resource.toStack((int) maxAmount));
                return maxAmount;
            }
        }
        return 0;
    }

    @Override
    public long extractSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext transaction) {
        updateSnapshots(transaction);
        // Todo: handle overflow with stacks over 64
        this.stackList.set(slot, resource.toStack((int) maxAmount));
        return maxAmount;
    }

    public abstract void onChange();

    @Override
    public long insertSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext transaction) {
        updateSnapshots(transaction);
        this.stackList.set(slot, ItemStack.EMPTY);
        return maxAmount;
    }

    @Override
    public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        for (int i = 0; i < this.stackList.size(); i++) {
            this.stackList.set(i, ItemStack.EMPTY);
            return 0;
        }
        return 0;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemVariant stack, long amount) {
        return isCertifiedStack(stack.toStack((int) amount));
    }

    private boolean isValid(int slot, @NotNull ItemVariant variant, long amount) {
        return !variant.toStack().isEmpty() && this.stackList.get(slot).isEmpty() && isCertifiedStack(variant.toStack());
    }

    private boolean isCertifiedStack(ItemStack stack){
        if (ItemApiLookup.get(new ResourceLocation("fabric:item_storage"), Storage.asClass(), ContainerItemContext.class).find(stack, ContainerItemContext.withInitial(stack)) != null) return false;
        if (stack.getMaxStackSize() > 1) return false;
        return stack.hasTag() || stack.isDamageableItem() || stack.isEnchantable();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag compoundTag = new CompoundTag();
        for (int i = 0; i < this.stackList.size(); i++) {
            ItemStack stack = this.stackList.get(i);
            if (!stack.isEmpty()){
                compoundTag.put(i + "", NBTSerializer.serializeNBT(stack));
            }
        }
        return compoundTag;
    }

    private List<ItemStack> create(){
        List<ItemStack> stackList = new ArrayList<>();
        for (int i = 0; i < FunctionalStorageConfig.ARMORY_CABINET_SIZE; i++) {
            stackList.add(ItemStack.EMPTY);
        }
        return stackList;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.stackList = create();
        for (String allKey : nbt.getAllKeys()) {
            int pos = Integer.parseInt(allKey);
            if (pos < this.stackList.size()){
                this.stackList.set(pos, ItemStack.of(nbt.getCompound(allKey)));
            }
        }
    }

    @Override
    protected List<ItemStack> createSnapshot() {
        return this.stackList;
    }

    @Override
    protected void readSnapshot(List<ItemStack> snapshot) {
        this.stackList = snapshot;
    }

    @Override
    protected void onFinalCommit() {
        super.onFinalCommit();
        onChange();
    }

    @Override
    public Iterator<StorageView<ItemVariant>> iterator() {
        return null;
    }
}

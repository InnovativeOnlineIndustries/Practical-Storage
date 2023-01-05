package com.buuz135.functionalstorage.inventory;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.google.common.collect.ImmutableList;
import io.github.fabricators_of_create.porting_lib.extensions.INBTSerializable;
import io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionSuccessCallback;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlotExposedIterator;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlotExposedStorage;
import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class BigInventoryHandler extends SnapshotParticipant<List<BigInventoryHandler.BigStack>> implements SlotExposedStorage, INBTSerializable<CompoundTag>, ILockable {

    public static String BIG_ITEMS = "BigItems";
    public static String STACK = "Stack";
    public static String AMOUNT = "Amount";

    private final FunctionalStorage.DrawerType type;
    private List<BigStack> storedStacks;

    public BigInventoryHandler(FunctionalStorage.DrawerType type) {
        this.type = type;
        this.storedStacks = new ArrayList<>();
        for (int i = 0; i < type.getSlots(); i++) {
            this.storedStacks.add(i, new BigStack(ItemStack.EMPTY, 0));
        }
    }

    @Override
    public int getSlots() {
        if (isVoid()) return type.getSlots() + 1;
        return type.getSlots();
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        if (type.getSlots() == slot) return ItemStack.EMPTY;
        BigStack bigStack = this.storedStacks.get(slot);
        ItemStack copied = bigStack.getStack().copy();
        copied.setCount(isCreative() ? Integer.MAX_VALUE : (int) bigStack.getAmount());
        return copied;
    }

    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        for (int slot = 0; slot < this.storedStacks.size(); slot++) {
            if (isVoid() && type.getSlots() == slot && isVoidValid(resource.toStack()) || (isVoidValid(resource.toStack()) && isCreative()))
                continue;
            if (isValid(slot, resource.toStack())) {
                BigStack bigStack = this.storedStacks.get(slot);
                long inserted = Math.min(getSlotLimit(slot) - bigStack.getAmount(), maxAmount);
                updateSnapshots(transaction);
                if (bigStack.getStack().isEmpty())
                    bigStack.setStack(ItemHandlerHelper.copyStackWithSize(resource.toStack(), resource.toStack().getMaxStackSize()));
                bigStack.setAmount(Math.min(bigStack.getAmount() + inserted, getSlotLimit(slot)));
                TransactionSuccessCallback.onSuccess(transaction, this::onChange);
                if (isVoid()) return 0;
                return inserted;
            }
        }

        return 0;
    }

    @Override
    public long insertSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext transaction) {
        if (isVoid() && type.getSlots() == slot && isVoidValid(resource.toStack()) || (isVoidValid(resource.toStack()) && isCreative()))
            return 0;
        if (isValid(slot, resource.toStack())) {
            BigStack bigStack = this.storedStacks.get(slot);
            long inserted = Math.min(getSlotLimit(slot) - bigStack.getAmount(), maxAmount);
            updateSnapshots(transaction);
            if (bigStack.getStack().isEmpty())
                bigStack.setStack(ItemHandlerHelper.copyStackWithSize(resource.toStack(), resource.toStack().getMaxStackSize()));
            bigStack.setAmount(Math.min(bigStack.getAmount() + inserted, getSlotLimit(slot)));
            if (isVoid()) return 0;
            return inserted;
        }
        return 0;
    }

    @Override
    public long extractSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext transaction) {
        if (maxAmount == 0 || type.getSlots() == slot) return 0;
        if (slot < type.getSlots()){
            BigStack bigStack = this.storedStacks.get(slot);
            if (bigStack.getStack().isEmpty()) return 0;
            if (bigStack.getAmount() <= maxAmount) {
                ItemStack out = bigStack.getStack().copy();
                long newAmount = bigStack.getAmount();
                updateSnapshots(transaction);
                if (!isCreative()) {
                    if (!isLocked()) bigStack.setStack(ItemStack.EMPTY);
                    bigStack.setAmount(0);
                }
                out.setCount((int) newAmount);
                return newAmount;
            } else {
                if (!isCreative()) {
                    updateSnapshots(transaction);
                    bigStack.setAmount(bigStack.getAmount() - maxAmount);
                }
                return maxAmount;
            }
        }
        return 0;
    }

    @Override
    public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        for (int slot = 0; slot < this.storedStacks.size(); slot++) {
            if (maxAmount == 0 || type.getSlots() == slot) continue;
            if (slot < type.getSlots()){
                BigStack bigStack = this.storedStacks.get(slot);
                if (bigStack.getStack().isEmpty()) continue;
                if (bigStack.getAmount() <= maxAmount) {
                    ItemStack out = bigStack.getStack().copy();
                    long newAmount = bigStack.getAmount();

                    if (!isCreative()) {
                        if (!isLocked()) bigStack.setStack(ItemStack.EMPTY);
                        updateSnapshots(transaction);
                        bigStack.setAmount(0);
                    }
                    out.setCount((int) newAmount);
                    return newAmount;
                } else {
                    if (!isCreative()) {
                        updateSnapshots(transaction);
                        bigStack.setAmount(bigStack.getAmount() - maxAmount);
                    }
                    return maxAmount;
                }
            }
        }
        return 0;
    }

    @Override
    protected void onFinalCommit() {
        onChange();
    }

    @Override
    public int getSlotLimit(int slot) {
        if (isCreative()) return Integer.MAX_VALUE;
        if (type.getSlots() == slot) return Integer.MAX_VALUE;
        double stackSize = 1;
        if (!getStoredStacks().get(slot).getStack().isEmpty()) {
            stackSize = getStoredStacks().get(slot).getStack().getMaxStackSize() / 64D;
        }
        if (hasDowngrade()) return (int) Math.floor(64 * stackSize);
        return (int) Math.floor(Math.min(Integer.MAX_VALUE, type.getSlotAmount() * (long) getMultiplier()) * stackSize);
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemVariant stack, long amount) {
        return !stack.toStack().isEmpty();
    }

    private boolean isValid(int slot, @Nonnull ItemStack stack){
        if (slot < type.getSlots()){
            BigStack bigStack = this.storedStacks.get(slot);
            ItemStack fl = bigStack.getStack();
            if (isLocked() && fl.isEmpty()) return false;
            return fl.isEmpty() || (fl.sameItem(stack) && ItemStack.tagMatches(fl, stack));
        }
        return false;
    }

    private boolean isVoidValid(ItemStack stack){
        for (BigStack storedStack : this.storedStacks) {
            if (storedStack.getStack().sameItem(stack) && ItemStack.tagMatches(storedStack.getStack(), stack)) return true;
        }
        return false;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag compoundTag = new CompoundTag();
        CompoundTag items = new CompoundTag();
        for (int i = 0; i < this.storedStacks.size(); i++) {
            CompoundTag bigStack = new CompoundTag();
            bigStack.put(STACK, NBTSerializer.serializeNBT(this.storedStacks.get(i).getStack()));
            bigStack.putLong(AMOUNT, this.storedStacks.get(i).getAmount());
            items.put(i + "", bigStack);
        }
        compoundTag.put(BIG_ITEMS, items);
        return compoundTag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        for (String allKey : nbt.getCompound(BIG_ITEMS).getAllKeys()) {
            this.storedStacks.get(Integer.parseInt(allKey)).setStack(ItemStack.of(nbt.getCompound(BIG_ITEMS).getCompound(allKey).getCompound(STACK)));
            this.storedStacks.get(Integer.parseInt(allKey)).setAmount(nbt.getCompound(BIG_ITEMS).getCompound(allKey).getLong(AMOUNT));
        }
    }

    @Override
    protected List<BigStack> createSnapshot() {
        List<BigStack> copied = new ArrayList<>();
        storedStacks.forEach(bigStack -> copied.add(new BigStack(bigStack.getStack(), bigStack.getAmount())));
        return copied;
    }

    @Override
    protected void readSnapshot(List<BigStack> snapshot) {
        this.storedStacks = snapshot;
    }

    @Override
    public Iterator<StorageView<ItemVariant>> iterator() {
        return new SlotExposedIterator(this);
    }

    public abstract void onChange();

    public abstract int getMultiplier();

    public abstract boolean isVoid();

    public abstract boolean hasDowngrade();

    public abstract boolean isLocked();

    public abstract boolean isCreative();

    public List<BigStack> getStoredStacks() {
        return storedStacks;
    }

    public static class BigStack {

        private ItemStack stack;
        private long amount;

        public BigStack(ItemStack stack, long amount) {
            this.stack = stack.copy();
            this.amount = amount;
        }

        public ItemStack getStack() {
            return stack;
        }

        public void setStack(ItemStack stack) {
            this.stack = stack.copy();
        }

        public long getAmount() {
            return amount;
        }

        public void setAmount(long amount) {
            this.amount = amount;
        }
    }
}

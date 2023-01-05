package com.buuz135.functionalstorage.inventory;

import com.buuz135.functionalstorage.util.CompactingUtil;
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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class CompactingInventoryHandler extends SnapshotParticipant<Long> implements SlotExposedStorage, INBTSerializable<CompoundTag>, ILockable {

    public static String PARENT = "Parent";
    public static String BIG_ITEMS = "BigItems";
    public static String STACK = "Stack";
    public static String AMOUNT = "Amount";

    public int totalAmount;

    private long amount;
    private ItemStack parent;
    private List<CompactingUtil.Result> resultList;
    private int slots;

    public CompactingInventoryHandler(int slots) {
        this.resultList = new ArrayList<>();
        this.slots = slots;
        this.totalAmount = 512;
        for (int i = 0; i < slots - 1; i++) {
            this.totalAmount *= 9;
        }
        for (int i = 0; i < slots; i++) {
            this.resultList.add(i, new CompactingUtil.Result(ItemStack.EMPTY, 1));
        }
        this.parent = ItemStack.EMPTY;
    }

    @Override
    public int getSlots() {
        if (isVoid()) return this.slots + 1;
        return this.slots;
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot == this.slots) return ItemStack.EMPTY;
        CompactingUtil.Result bigStack = this.resultList.get(slot);
        ItemStack copied = bigStack.getResult().copy();
        copied.setCount(isCreative() ? Integer.MAX_VALUE : (int) (this.amount / bigStack.getNeeded()));
        return copied;
    }

    @Override
    public long insertSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext transaction) {
        if (isVoid() && slot == this.slots && isVoidValid(resource.toStack()) || (isVoidValid(resource.toStack()) && isCreative()))
            return 0;
        if (isValid(slot, resource.toStack())) {
            updateSnapshots(transaction);
            CompactingUtil.Result result = this.resultList.get(slot);
            long inserted = Math.min(getSlotLimit(slot) * result.getNeeded() - amount, maxAmount * result.getNeeded());
            this.amount = Math.min(this.amount + inserted, totalAmount * getMultiplier());
            TransactionSuccessCallback.onSuccess(transaction, this::onChange);
            if (isVoid()) return 0;
            return inserted / result.getNeeded();
        }
        return 0;
    }

    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        for (int slot = 0; slot < this.resultList.size(); slot++) {
            if (isVoid() && slot == 3 && isVoidValid(resource.toStack()) || (isVoidValid(resource.toStack()) && isCreative())) continue;
            if (isValid(slot, resource.toStack())) {
                updateSnapshots(transaction);
                CompactingUtil.Result result = this.resultList.get(slot);
                long inserted = Math.min(getSlotLimit(slot) * result.getNeeded() - amount, maxAmount * result.getNeeded());
                this.amount = Math.min(this.amount + inserted, TOTAL_AMOUNT * getMultiplier());
                TransactionSuccessCallback.onSuccess(transaction, this::onChange);
                if (isVoid()) return 0;
                return inserted / result.getNeeded();
            }
        }
        return 0;
    }

    private boolean isVoidValid(ItemStack stack) {
        for (CompactingUtil.Result result : this.resultList) {
            if (result.getResult().sameItem(stack) && ItemStack.tagMatches(result.getResult(), stack)) return true;
        }
        return false;
    }

    public boolean isSetup(){
        return !this.resultList.get(this.resultList.size() -1).getResult().isEmpty();
    }

    public void setup(CompactingUtil compactingUtil){
        this.resultList = compactingUtil.getResults();
        this.parent = compactingUtil.getResults().get(0).getResult();
        if (this.parent.isEmpty()) {
            this.parent = compactingUtil.getResults().get(1).getResult();
        }
        if (this.parent.isEmpty() && compactingUtil.getResults().size() >= 3) {
            this.parent = compactingUtil.getResults().get(2).getResult();
        }
        onChange();
    }

    public void reset(){
        if (isLocked()) return;
        this.resultList.forEach(result -> {
            result.setResult(ItemStack.EMPTY);
            result.setNeeded(1);
        });
    }

    public long getAmount() {
        return amount;
    }

    @Override
    public long extractSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext transaction) {
        if (amount == 0 || slot == this.slots) return 0;
        if (slot < this.slots) {
            CompactingUtil.Result bigStack = this.resultList.get(slot);
            if (bigStack.getResult().isEmpty()) return 0;
            long stackAmount = bigStack.getNeeded() * amount;
            if (stackAmount >= this.amount) {
                ItemStack out = bigStack.getResult().copy();
                long newAmount = Mth.lfloor(this.amount / bigStack.getNeeded());
                if (!isCreative()) {
                    updateSnapshots(transaction);
                    this.amount -= (newAmount * bigStack.getNeeded());
                    if (this.amount == 0) reset();
                    TransactionSuccessCallback.onSuccess(transaction, this::onChange);
                    out.setCount((int) newAmount);
                    return newAmount;
                }
            } else {
                if (!isCreative()) {
                    updateSnapshots(transaction);
                    this.amount -= stackAmount;
                    TransactionSuccessCallback.onSuccess(transaction, this::onChange);
                }
                return amount;
            }
        }
        return 0;
    }

    @Override
    public long extract(ItemVariant resource, long amount, TransactionContext transaction) {
        for (int slot = 0; slot < this.resultList.size(); slot++) {
            if (amount == 0 || slot == 3) continue;
            if (slot < 3){
                CompactingUtil.Result bigStack = this.resultList.get(slot);
                if (bigStack.getResult().isEmpty()) continue;
                long stackAmount = bigStack.getNeeded() * amount;
                if (stackAmount >= this.amount) {
                    ItemStack out = bigStack.getResult().copy();
                    long newAmount = Mth.lfloor(this.amount / bigStack.getNeeded());
                    if (!isCreative()) {
                        updateSnapshots(transaction);
                        this.amount -= (newAmount * bigStack.getNeeded());
                        if (this.amount == 0) reset();
                        TransactionSuccessCallback.onSuccess(transaction, this::onChange);
                        out.setCount((int) newAmount);
                        return newAmount;
                    }
                } else {
                    if (!isCreative()) {
                        updateSnapshots(transaction);
                        this.amount -= stackAmount;
                        TransactionSuccessCallback.onSuccess(transaction, this::onChange);
                    }
                    return amount;
                }
            }
        }
        return 0;
    }

    @Override
    public int getSlotLimit(int slot) {
        if (isCreative()) return Integer.MAX_VALUE;
        if (slot == this.slots) return Integer.MAX_VALUE;
        int total = totalAmount;
        if (hasDowngrade()) total = 64 * 9 * 9;
        return (int) Math.min(Integer.MAX_VALUE, Math.floor((total * getMultiplier()) / this.resultList.get(slot).getNeeded()));
    }

    public int getSlotLimitBase(int slot) {
        if (slot == this.slots) return Integer.MAX_VALUE;
        int total = totalAmount;
        if (hasDowngrade()) total = 64 * 9 * 9;
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(total / this.resultList.get(slot).getNeeded()));
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemVariant stack, long amount) {
        return isSetup() && !stack.toStack().isEmpty();
    }

    private boolean isValid(int slot, @Nonnull ItemStack stack){
        if (slot < this.slots) {
            CompactingUtil.Result bigStack = this.resultList.get(slot);
            ItemStack fl = bigStack.getResult();
            return !fl.isEmpty() && fl.sameItem(stack) && ItemStack.tagMatches(fl, stack);
        }
        return false;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.put(PARENT, NBTSerializer.serializeNBT(this.getParent()));
        compoundTag.putLong(AMOUNT, this.amount);
        CompoundTag items = new CompoundTag();
        for (int i = 0; i < this.resultList.size(); i++) {
            CompoundTag bigStack = new CompoundTag();
            bigStack.put(STACK, NBTSerializer.serializeNBT(this.resultList.get(i).getResult()));
            bigStack.putLong(AMOUNT, this.resultList.get(i).getNeeded());
            items.put(i + "", bigStack);
        }
        compoundTag.put(BIG_ITEMS, items);
        return compoundTag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.parent = ItemStack.of(nbt.getCompound(PARENT));
        this.amount = nbt.getLong(AMOUNT);
        for (String allKey : nbt.getCompound(BIG_ITEMS).getAllKeys()) {
            this.resultList.get(Integer.parseInt(allKey)).setResult(ItemStack.of(nbt.getCompound(BIG_ITEMS).getCompound(allKey).getCompound(STACK)));
            this.resultList.get(Integer.parseInt(allKey)).setNeeded(Math.max(1, nbt.getCompound(BIG_ITEMS).getCompound(allKey).getLong(AMOUNT)));
        }
    }

    @Override
    public Iterator<StorageView<ItemVariant>> iterator() {
        return new SlotExposedIterator(this);
    }

    @Override
    protected Long createSnapshot() {
        return this.amount;
    }

    @Override
    protected void readSnapshot(Long snapshot) {
        this.amount = snapshot;
    }

    public abstract void onChange();

    public abstract int getMultiplier();

    public abstract boolean isVoid();

    public List<CompactingUtil.Result> getResultList() {
        return resultList;
    }

    public ItemStack getParent() {
        return parent;
    }

    public abstract boolean hasDowngrade();

    public abstract boolean isCreative();
}

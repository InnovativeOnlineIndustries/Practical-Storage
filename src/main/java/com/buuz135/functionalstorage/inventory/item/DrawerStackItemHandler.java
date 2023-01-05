package com.buuz135.functionalstorage.inventory.item;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.inventory.BigInventoryHandler;
import com.buuz135.functionalstorage.item.StorageUpgradeItem;
import io.github.fabricators_of_create.porting_lib.extensions.INBTSerializable;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlotExposedIterator;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlotExposedStorage;
import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.buuz135.functionalstorage.inventory.BigInventoryHandler.*;

public class DrawerStackItemHandler extends SnapshotParticipant<List<BigInventoryHandler.BigStack>> implements SlotExposedStorage, INBTSerializable<CompoundTag> {

    private List<BigInventoryHandler.BigStack> storedStacks;
    private ContainerItemContext context;
    private FunctionalStorage.DrawerType type;
    private int multiplier;
    private boolean downgrade;
    private boolean isVoid;

    public DrawerStackItemHandler(ContainerItemContext context, FunctionalStorage.DrawerType drawerType) {
        this.context = context;
        this.storedStacks = new ArrayList<>();
        this.type = drawerType;
        this.multiplier = 1;
        this.downgrade = false;
        this.isVoid = false;
        for (int i = 0; i < drawerType.getSlots(); i++) {
            this.storedStacks.add(i, new BigInventoryHandler.BigStack(ItemStack.EMPTY, 0));
        }
        if (context.getItemVariant().hasNbt()) {
            deserializeNBT(context.getItemVariant().getNbt().getCompound("Tile").getCompound("handler"));
            for (Tag tag : context.getItemVariant().copyOrCreateNbt().getCompound("Tile").getCompound("storageUpgrades").getList("Items", Tag.TAG_COMPOUND)) {
                ItemStack itemStack = ItemStack.of((CompoundTag) tag);
                if (itemStack.getItem() instanceof StorageUpgradeItem) {
                    if (multiplier == 1) multiplier = ((StorageUpgradeItem) itemStack.getItem()).getStorageMultiplier();
                    else multiplier *= ((StorageUpgradeItem) itemStack.getItem()).getStorageMultiplier();
                }
                if (itemStack.getItem().equals(FunctionalStorage.STORAGE_UPGRADES.get(StorageUpgradeItem.StorageTier.IRON).get())) {
                    this.downgrade = true;
                }
            }
            for (Tag tag : context.getItemVariant().copyOrCreateNbt().getCompound("Tile").getCompound("utilityUpgrades").getList("Items", Tag.TAG_COMPOUND)) {
                ItemStack itemStack = ItemStack.of((CompoundTag) tag);
                if (itemStack.getItem().equals(FunctionalStorage.VOID_UPGRADE.get())) {
                    this.isVoid = true;
                }
            }
        }
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
    public int getSlots() {
        return type.getSlots();
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        BigStack bigStack = this.storedStacks.get(slot);
        ItemStack copied = bigStack.getStack().copy();
        copied.setCount((int) bigStack.getAmount());
        return copied;
    }

    @Override
    public long insertSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext transaction) {
        if (isValid(slot, context.getItemVariant().toStack())) {
            updateSnapshots(transaction);
            BigStack bigStack = this.storedStacks.get(slot);
            long inserted = Math.min(getSlotLimit(slot) - bigStack.getAmount(), context.getAmount());
            bigStack.setStack(context.getItemVariant().toStack((int) context.getAmount()));
            bigStack.setAmount(Math.min(bigStack.getAmount() + inserted, getSlotLimit(slot)));
            onChange(transaction);
            if (isVoid()) return 0;
            return inserted;
        }
        return 0;
    }

    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        ItemStack stack = resource.toStack((int) maxAmount);
        for (int slot = 0; slot < this.storedStacks.size(); slot++) {
            if (isValid(slot, stack)) {
                updateSnapshots(transaction);
                BigStack bigStack = this.storedStacks.get(slot);
                long inserted = Math.min(getSlotLimit(slot) - bigStack.getAmount(), stack.getCount());
                bigStack.setStack(stack);
                bigStack.setAmount(Math.min(bigStack.getAmount() + inserted, getSlotLimit(slot)));
                onChange(transaction);
                if (isVoid()) return 0;
                return inserted;
            }
        }
        return 0;
    }

    private boolean isVoid() {
        return true;
    }

    private void onChange(TransactionContext tx) {
        ItemStack newStack = context.getItemVariant().toStack();
        if (newStack.getOrCreateTag().contains("Tile"))
            newStack.getOrCreateTag().put("Tile", new CompoundTag());
        newStack.getOrCreateTag().getCompound("Tile").put("handler", serializeNBT());
        try (Transaction nested = tx.openNested()) {
            if (context.exchange(ItemVariant.of(newStack), 1, nested) == 1)
                nested.commit();
        }
    }

    private boolean isValid(int slot, @Nonnull ItemStack stack) {
        if (slot < type.getSlots()) {
            BigStack bigStack = this.storedStacks.get(slot);
            ItemStack fl = bigStack.getStack();
            return fl.isEmpty() || (fl.sameItem(stack) && ItemStack.tagMatches(fl, stack));
        }
        return false;
    }

    @Override
    public long extractSlot(int slot, ItemVariant resource, long amount, TransactionContext transaction) {
        if (amount == 0) return 0;
        if (slot < type.getSlots()) {
            BigStack bigStack = this.storedStacks.get(slot);
            if (bigStack.getStack().isEmpty()) return 0;
            updateSnapshots(transaction);
            if (bigStack.getAmount() <= amount) {
                ItemStack out = bigStack.getStack().copy();
                long newAmount = bigStack.getAmount();
                if (!isLocked()) bigStack.setStack(ItemStack.EMPTY);
                bigStack.setAmount(0);
                onChange(transaction);
                out.setCount((int) newAmount);
                return newAmount;
            } else {
                bigStack.setAmount(bigStack.getAmount() - amount);
                onChange(transaction);
            }
            return amount;
        }
        return 0;
    }

    @Override
    public long extract(ItemVariant resource, long amount, TransactionContext transaction) {
        if (amount == 0) return 0;
        for (int slot = 0; slot < this.storedStacks.size(); slot++) {
            if (slot < type.getSlots()) {
                BigStack bigStack = this.storedStacks.get(slot);
                if (bigStack.getStack().isEmpty()) continue;
                updateSnapshots(transaction);
                if (bigStack.getAmount() <= amount) {
                    ItemStack out = bigStack.getStack().copy();
                    long newAmount = bigStack.getAmount();
                    if (!isLocked()) bigStack.setStack(ItemStack.EMPTY);
                    bigStack.setAmount(0);
                    onChange(transaction);
                    out.setCount((int) newAmount);
                    return newAmount;
                } else {
                    bigStack.setAmount(bigStack.getAmount() - amount);
                    onChange(transaction);
                }
                return amount;
            }
        }
        return 0;
    }

    @Override
    protected List<BigStack> createSnapshot() {
        return this.storedStacks;
    }

    @Override
    protected void readSnapshot(List<BigStack> snapshot) {
        this.storedStacks = snapshot;
    }

    public boolean isLocked() {
        return true;
    }

    @Override
    public int getSlotLimit(int slot) {
        if (hasDowngrade()) return 64;
        return (int) Math.min(Integer.MAX_VALUE, type.getSlotAmount() * (long) getMultiplier());
    }

    private long getMultiplier() {
        return multiplier;
    }

    private boolean hasDowngrade() {
        return downgrade;
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemVariant variant, long amount) {
        return !variant.toStack().isEmpty();
    }

    public List<BigStack> getStoredStacks() {
        return storedStacks;
    }

    @Override
    public Iterator<StorageView<ItemVariant>> iterator() {
        return new SlotExposedIterator(this);
    }
}

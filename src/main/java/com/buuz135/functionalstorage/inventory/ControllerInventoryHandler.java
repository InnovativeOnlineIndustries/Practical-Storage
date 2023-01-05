package com.buuz135.functionalstorage.inventory;

import com.buuz135.functionalstorage.block.tile.DrawerControllerTile;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlotExposedIterator;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlotExposedStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class HandlerSlotSelector {
    SlotExposedStorage handler;
    int slot;

    public HandlerSlotSelector(SlotExposedStorage handler, int slot) {
        this.handler = handler;
        this.slot = slot;
    }

    public ItemStack getStackInSlot() {
        return handler.getStackInSlot(slot);
    }

    public long insertItem(@NotNull ItemVariant variant, long amount, TransactionContext tx) {
        return handler.insertSlot(slot, variant, amount, tx);
    }

    public long extractItem(ItemVariant variant, long amount, TransactionContext tx) {
        return handler.extractSlot(slot, variant, amount, tx);
    }

    public int getSlotLimit() {
        return handler.getSlotLimit(slot);
    }

    public boolean isItemValid(@NotNull ItemVariant variant, long amount) {
        return handler.isItemValid(slot, variant, amount);
    }
}

public abstract class ControllerInventoryHandler implements SlotExposedStorage {

    HandlerSlotSelector[] selectors;
    private int slots = 0;

    public ControllerInventoryHandler() {
        invalidateSlots();
    }

    @Override
    public int getSlots() {
        return slots;
    }

    public void invalidateSlots() {
        List<HandlerSlotSelector> selectors = new ArrayList<HandlerSlotSelector>();
        this.slots = 0;
        for (SlotExposedStorage handler : getDrawers().getItemHandlers()) {
            if (handler instanceof ControllerInventoryHandler) continue;
            int handlerSlots = handler.getSlots();
            for (int i = 0; i < handlerSlots; ++i) {
                selectors.add(new HandlerSlotSelector(handler, i));
            }
            this.slots += handlerSlots;
        }
        this.selectors = selectors.toArray(new HandlerSlotSelector[selectors.size()]);
    }

    private HandlerSlotSelector selectorForSlot(int slot) {
        return slot >= 0 && slot < selectors.length ? selectors[slot] : null;
    }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
        HandlerSlotSelector selector = selectorForSlot(slot);
        return null != selector ? selector.getStackInSlot() : ItemStack.EMPTY;
    }

    @Override
    public long insertSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext transaction) {
        HandlerSlotSelector selector = selectorForSlot(slot);
        return null != selector ? selector.insertItem(resource, maxAmount, transaction) : 0;
    }

    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        for (int slot = 0; slot < getSlots(); slot++) {
            HandlerSlotSelector selector = selectorForSlot(slot);
            if (selector == null) continue;
            return selector.insertItem(resource, maxAmount, transaction);
        }
        return 0;
    }

    @Override
    public long extractSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext transaction) {
        HandlerSlotSelector selector = selectorForSlot(slot);
        return null != selector ? selector.extractItem(resource, maxAmount, transaction) : 0;
    }

    @Override
    public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        for (int slot = 0; slot < getSlots(); slot++) {
            HandlerSlotSelector selector = selectorForSlot(slot);
            if (selector == null) continue;
            return selector.extractItem(resource, maxAmount, transaction);
        }
        return 0;
    }

    @Override
    public int getSlotLimit(int slot) {
        HandlerSlotSelector selector = selectorForSlot(slot);
        return null != selector ? selector.getSlotLimit() : 0;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemVariant variant, long amount) {
        HandlerSlotSelector selector = selectorForSlot(slot);
        return null != selector ? selector.isItemValid(variant, amount) : false;
    }

    @Override
    public Iterator<StorageView<ItemVariant>> iterator() {
        return new SlotExposedIterator(this);
    }

    public abstract DrawerControllerTile.ConnectedDrawers getDrawers();
}

package com.buuz135.functionalstorage.fluid;

import com.buuz135.functionalstorage.block.tile.DrawerControllerTile;
import com.buuz135.functionalstorage.inventory.ControllerInventoryHandler;
import io.github.fabricators_of_create.porting_lib.util.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public abstract class ControllerFluidHandler implements Storage<FluidVariant> {

    HandlerTankSelector[] selectors;
    private int tanks = 0;

    public ControllerFluidHandler() {
        invalidateSlots();
    }

    public void invalidateSlots() {
        List<HandlerTankSelector> selectors = new ArrayList<>();
        this.tanks = 0;
        for (BigFluidHandler handler : getDrawers().getFluidHandlers()) {
//            if (handler instanceof ControllerInventoryHandler) continue;
            int handlerTanks = handler.getTanks();
            for (int i = 0; i < handlerTanks; ++i) {
                selectors.add(new HandlerTankSelector(handler, i));
            }
            this.tanks += handlerTanks;
        }
        this.selectors = selectors.toArray(new HandlerTankSelector[selectors.size()]);
    }

    private HandlerTankSelector selectorForTank(int tank) {
        return tank >= 0 && tank < selectors.length ? selectors[tank] : null;
    }

    @Override
    public long insert(FluidVariant resource, long amount, TransactionContext tx) {
        for (HandlerTankSelector selector : this.selectors) {
            if (!selector.getStackInSlot().isEmpty() && selector.getStackInSlot().isFluidEqual(resource)) {
                return selector.insert(resource, amount, tx);
            }
        }
        for (HandlerTankSelector selector : this.selectors) {
            if (selector.getStackInSlot().isEmpty()) {
                return selector.insert(resource, amount, tx);
            }
        }
        return 0;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        for (HandlerTankSelector selector : this.selectors) {
            if (!selector.getStackInSlot().isEmpty() && selector.getStackInSlot().isFluidEqual(resource)) {
                return selector.extract(resource, maxAmount, transaction);
            }
        }
        for (HandlerTankSelector selector : this.selectors) {
            if (selector.getStackInSlot().isEmpty()) {
                return selector.extract(resource, maxAmount, transaction);
            }
        }
        return 0;
    }

    @Override
    public Iterator<StorageView<FluidVariant>> iterator() {
        return new CombinedIterator();
    }

    private class CombinedIterator implements Iterator<StorageView<FluidVariant>> {
        final Iterator<HandlerTankSelector> partIterator = List.of(selectors).iterator();
        // Always holds the next StorageView<T>, except during next() while the iterator is being advanced.
        Iterator<? extends StorageView<FluidVariant>> currentPartIterator = null;

        CombinedIterator() {
            advanceCurrentPartIterator();
        }

        @Override
        public boolean hasNext() {
            return currentPartIterator != null && currentPartIterator.hasNext();
        }

        @Override
        public StorageView<FluidVariant> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            StorageView<FluidVariant> returned = currentPartIterator.next();

            // Advance the current part iterator
            if (!currentPartIterator.hasNext()) {
                advanceCurrentPartIterator();
            }

            return returned;
        }

        private void advanceCurrentPartIterator() {
            while (partIterator.hasNext()) {
                this.currentPartIterator = partIterator.next().handler.iterator();

                if (this.currentPartIterator.hasNext()) {
                    break;
                }
            }
        }
    }

    public abstract DrawerControllerTile.ConnectedDrawers getDrawers();
}

class HandlerTankSelector {
    BigFluidHandler handler;
    int slot;

    public HandlerTankSelector(BigFluidHandler handler, int slot) {
        this.handler = handler;
        this.slot = slot;
    }

    public FluidStack getStackInSlot() {
        return handler.getFluidInTank(slot);
    }

    public long insert(@NotNull FluidVariant variant, long amount, TransactionContext tx) {
        return handler.insert(variant, amount, tx);
    }

    public long extract(FluidVariant variant, long amount, TransactionContext tx) {
        return handler.extract(variant, amount, tx);
    }

    public long getCapacity() {
        return handler.getTankCapacity(slot);
    }

    public boolean isFluidValid(@NotNull FluidStack stack) {
        return handler.isFluidValid(slot, stack);
    }
}

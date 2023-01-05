package com.buuz135.functionalstorage.fluid;

import io.github.fabricators_of_create.porting_lib.extensions.INBTSerializable;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import io.github.fabricators_of_create.porting_lib.util.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.function.Predicate;

public abstract class BigFluidHandler extends SnapshotParticipant<BigFluidHandler.CustomFluidTank[]> implements Storage<FluidVariant>, INBTSerializable<CompoundTag> {

    private CustomFluidTank[] tanks;
    private FluidStack[] filterStack;
    private long capacity;

    public BigFluidHandler(int size, long capacity) {
        this.tanks = new CustomFluidTank[size];
        this.filterStack = new FluidStack[size];
        for (int i = 0; i < this.tanks.length; i++) {
            this.filterStack[i] = FluidStack.EMPTY;
            int finalI = i;
            this.tanks[i] = new CustomFluidTank(capacity, fluidStack -> {
                if (isDrawerLocked()) {
                    return fluidStack.isFluidEqual(this.filterStack[finalI]);
                }
                return true;
            });
        }
        this.capacity = capacity;
    }

    public CustomFluidTank[] getTankList() {
        return this.tanks;
    }

    public int getTanks() {
        return this.tanks.length;
    }

    public @NotNull FluidStack getFluidInTank(int tank) {
        return this.tanks[tank].getFluidInTank();
    }

    public long getTankCapacity(int tank) {
        return this.tanks[tank].getCapacity();
    }

    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return this.tanks[tank].isFluidValid(stack);
    }

    @Override
    public long insert(FluidVariant resource, long amount, TransactionContext action) {
        for (CustomFluidTank tank : tanks) {
            if (!tank.getFluid().isEmpty() && tank.simulateInsert(resource, amount, action) != 0) {
                updateSnapshots(action);
                return tank.insert(resource, amount, action);
            }
        }
        for (CustomFluidTank tank : tanks) {
            if (tank.getFluid().isEmpty() && tank.simulateInsert(resource, amount, action) != 0) {
                updateSnapshots(action);
                return tank.insert(resource, amount, action);
            }
        }
        return 0;
    }

    @Nonnull
    @Override
    public long extract(FluidVariant resource, long amount, TransactionContext action) {
        for (CustomFluidTank tank : tanks) {
            if (!tank.getFluid().isEmpty() && tank.getFluid().isFluidEqual(resource) && !(tank.simulateExtract(resource, amount, action) <= 0L)) {
                updateSnapshots(action);
                return tank.extract(resource, amount, action);
            }
        }
        for (CustomFluidTank tank : tanks) {
            if (!(tank.simulateExtract(resource, amount, action) <= 0L)) {
                updateSnapshots(action);
                return tank.extract(resource, amount, action);
            }
        }
        return 0;
    }

    @Override
    protected void onFinalCommit() {
        onChange();
    }

    @Override
    protected CustomFluidTank[] createSnapshot() {
        CustomFluidTank[] array = new CustomFluidTank[this.tanks.length];
        System.arraycopy(this.tanks, 0, array, 0, this.tanks.length);
        return array;
    }

    @Override
    protected void readSnapshot(CustomFluidTank[] snapshot) {
        this.tanks = snapshot;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
        for (CustomFluidTank tank : this.tanks) {
            tank.setCapacity(capacity);
            if (!tank.getFluid().isEmpty()) tank.getFluid().setAmount(Math.min(tank.getFluidAmount(), capacity));
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag compoundTag = new CompoundTag();
        for (int i = 0; i < this.tanks.length; i++) {
            compoundTag.put(i + "", this.tanks[i].writeToNBT(new CompoundTag()));
            compoundTag.put("Locked" + i, this.filterStack[i].writeToNBT(new CompoundTag()));
        }
        compoundTag.putLong("Capacity", this.capacity);
        return compoundTag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.capacity = nbt.getLong("Capacity");
        for (int i = 0; i < this.tanks.length; i++) {
            this.tanks[i].readFromNBT(nbt.getCompound(i + ""));
            this.tanks[i].setCapacity(this.capacity);
            this.filterStack[i] = FluidStack.loadFluidStackFromNBT(nbt.getCompound("Locked" + i));
        }
    }

    public abstract void onChange();

    public abstract boolean isDrawerLocked();

    public abstract boolean isDrawerVoid();

    public abstract boolean isDrawerCreative();

    public void lockHandler() {
        for (int i = 0; i < this.tanks.length; i++) {
            this.filterStack[i] = this.tanks[i].getFluid().copy();
            if (!this.filterStack[i].isEmpty()) this.filterStack[i].setAmount(1);
        }
    }

    public FluidStack[] getFilterStack() {
        return filterStack;
    }

    @Override
    public Iterator<StorageView<FluidVariant>> iterator() {
        return new BigFluidIterator(this);
    }

    public class BigFluidIterator implements Iterator<StorageView<FluidVariant>> {
        protected int index = 0;
        protected BigFluidHandler handler;

        public BigFluidIterator(BigFluidHandler handler) {
            this.handler = handler;
        }

        public boolean hasNext() {
            return this.index < this.handler.getTanks();
        }

        public StorageView<FluidVariant> next() {
            ++this.index;
            return new TankView(this.handler, this.index - 1);
        }
    }

    public class TankView extends SnapshotParticipant<FluidStack> implements StorageView<FluidVariant> {

        protected final BigFluidHandler handler;
        protected final int index;

        public TankView(BigFluidHandler handler, int index) {
            this.handler = handler;
            this.index = index;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            FluidStack stack = getFluidInTank(index);
            long extracted = 0;

            if (resource.isOf(stack.getFluid()) && resource.nbtMatches(stack.getTag())) {
                extracted = Math.min(stack.getAmount(), maxAmount);
                if (extracted > 0) {
                    updateSnapshots(transaction);
                    long remaining = stack.getAmount() - extracted;
                    if (remaining <= 0) {
                        handler.tanks[index].setFluid(FluidStack.EMPTY);
                    } else {
                        FluidStack newStack = stack.copy();
                        newStack.setAmount(remaining);
                        handler.tanks[index].setFluid(newStack);
                    }
                }
            }
            return extracted;
        }

        @Override
        public boolean isResourceBlank() {
            return getResource().isBlank();
        }

        @Override
        public FluidVariant getResource() {
            return handler.getFluidInTank(index).getType();
        }

        @Override
        public long getAmount() {
            return handler.getFluidInTank(index).getAmount();
        }

        @Override
        public long getCapacity() {
            return handler.getTankCapacity(index);
        }

        @Override
        protected FluidStack createSnapshot() {
            return handler.getFluidInTank(index).copy();
        }

        @Override
        protected void readSnapshot(FluidStack snapshot) {
            handler.tanks[index].setFluid(snapshot);
        }
    }

    public class CustomFluidTank extends FluidTank {


        public CustomFluidTank(long capacity) {
            super(capacity);
        }

        public CustomFluidTank(long capacity, Predicate<FluidStack> validator) {
            super(capacity, validator);
        }

        @Override
        public long insert(FluidVariant insertedVariant, long maxAmount, TransactionContext transaction) {
            long amount = super.insert(insertedVariant, maxAmount, transaction);
            if (isDrawerVoid()
                    && ((isDrawerLocked() && isFluidValid(new FluidStack(insertedVariant, maxAmount))) || (!getFluid().isEmpty() && getFluid().isFluidEqual(insertedVariant))))
                return maxAmount;
            return amount;
        }


        public @NotNull FluidStack getFluidInTank() {
            FluidStack stack = super.getFluid();
            if (isDrawerCreative()) stack.setAmount(Long.MAX_VALUE);
            return stack;
        }

        @Override
        public long extract(FluidVariant extractedVariant, long maxAmount, TransactionContext transaction) {
            if (isDrawerCreative()) return maxAmount;
            return super.extract(extractedVariant, maxAmount, transaction);
        }

        @Override
        public long getCapacity() {
            return isDrawerCreative() ? Long.MAX_VALUE : super.getCapacity();
        }

        @Override
        public @NotNull FluidStack getFluid() {
            FluidStack stack = super.getFluid();
            if (isDrawerCreative()) stack.setAmount(Long.MAX_VALUE);
            return stack;
        }

        @Override
        public long getFluidAmount() {
            return isDrawerCreative() ? Long.MAX_VALUE : super.getFluidAmount();
        }
    }
}

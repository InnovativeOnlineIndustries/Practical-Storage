package com.buuz135.functionalstorage.util;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlotExposedStorage;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class FabricUtil {
    public static final ItemApiLookup<Storage<ItemVariant>, ContainerItemContext> CONTEXT_ITEM_API_LOOKUP =
            ItemApiLookup.get(new ResourceLocation("fabric:item_storage"), Storage.asClass(), ContainerItemContext.class);

    public static long insertSlotSimulated(SlotExposedStorage storage, int slot, ItemStack stack) {
        try (Transaction tx = TransferUtil.getTransaction()) {
            return stack.getCount() - storage.insertSlot(slot, ItemVariant.of(stack), stack.getCount(), tx);
        }
    }

    public static ItemStack insertSlot(SlotExposedStorage storage, int slot, ItemStack stack) {
        try (Transaction tx = TransferUtil.getTransaction()) {
            ItemStack newStack = stack.copy();
            long inserted = storage.insertSlot(slot, ItemVariant.of(stack), stack.getCount(), tx);
            newStack.setCount(stack.getCount() - (int) inserted);
            tx.commit();
            return newStack;
        }
    }

    public static long extractSlot(SlotExposedStorage storage, int slot, long amount) {
        try (Transaction tx = TransferUtil.getTransaction()) {
            long extracted = storage.extractSlot(slot, ItemVariant.of(storage.getStackInSlot(slot)), amount, tx);
            tx.commit();
            return extracted;
        }
    }

    public static long simulateExtractSlot(SlotExposedStorage storage, int slot, long amount) {
        try (Transaction tx = TransferUtil.getTransaction()) {
            return storage.extractSlot(slot, ItemVariant.of(storage.getStackInSlot(slot)), amount, tx);
        }
    }

    public static <T> long simulateExtractView(StorageView<T> view, T resource, long amount) {
        try (Transaction tx = TransferUtil.getTransaction()) {
            return view.extract(resource, amount, tx);
        }
    }
}

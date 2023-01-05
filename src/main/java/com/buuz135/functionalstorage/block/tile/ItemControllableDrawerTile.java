package com.buuz135.functionalstorage.block.tile;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.item.StorageUpgradeItem;
import com.buuz135.functionalstorage.item.UpgradeItem;
import com.buuz135.functionalstorage.util.FabricUtil;
import com.hrznstudio.titanium.block.BasicTileBlock;
import com.hrznstudio.titanium.component.inventory.InventoryComponent;
import com.hrznstudio.titanium.util.RayTraceUtils;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlotExposedStorage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.HashMap;
import java.util.UUID;

public abstract class ItemControllableDrawerTile<T extends ItemControllableDrawerTile<T>> extends ControllableDrawerTile<T> {

    private static HashMap<UUID, Long> INTERACTION_LOGGER = new HashMap<>();

    public ItemControllableDrawerTile(BasicTileBlock<T> base, BlockEntityType<T> entityType, BlockPos pos, BlockState state) {
        super(base, entityType, pos, state);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void initClient() {
        super.initClient();
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state, T blockEntity) {
        super.serverTick(level, pos, state, blockEntity);
        if (level.getGameTime() % 4 == 0) {
            for (int i = 0; i < this.getUtilityUpgrades().getSlots(); i++) {
                ItemStack stack = this.getUtilityUpgrades().getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Item item = stack.getItem();
                    if (item.equals(FunctionalStorage.PULLING_UPGRADE.get())) {
                        Direction direction = UpgradeItem.getDirection(stack);
                        FabricUtil.getStorage(ItemStorage.SIDED, level, pos.relative(direction), direction.getOpposite()).ifPresent(iItemHandler -> {
                            for (StorageView<ItemVariant> view : iItemHandler) {
                                long pulledStack = FabricUtil.simulateExtractView(view, view.getResource(), 2);
                                if (pulledStack <= 0) continue;
                                boolean hasWorked = false;
                                for (int ourSlot = 0; ourSlot < this.getStorage().getSlots(); ourSlot++) {
                                    long simulated = FabricUtil.insertSlotSimulated(getStorage(), ourSlot, view.getResource().toStack((int) pulledStack));
                                    if (simulated != pulledStack) {
                                        try (Transaction tx = TransferUtil.getTransaction()) {
                                            long extracted = view.extract(view.getResource(), pulledStack - simulated, tx);
                                            FabricUtil.insertSlot(getStorage(), ourSlot, view.getResource().toStack((int) extracted));
                                            tx.commit();
                                        }
                                        hasWorked = true;
                                        break;
                                    }
                                }
                                if (hasWorked) break;
                            }
                        });
                    }
                    if (item.equals(FunctionalStorage.PUSHING_UPGRADE.get())) {
                        Direction direction = UpgradeItem.getDirection(stack);
                        FabricUtil.getStorage(ItemStorage.SIDED, level, pos.relative(direction), direction.getOpposite()).ifPresent(otherHandler -> {
                            for (int otherSlot = 0; otherSlot < getStorage().getSlots(); otherSlot++) {
                                long pulledStack = FabricUtil.simulateExtractSlot(getStorage(), otherSlot, 2);
                                if (pulledStack <= 0) continue;
                                long simulated = pulledStack - otherHandler.simulateInsert(ItemVariant.of(getStorage().getStackInSlot(otherSlot)), pulledStack, null);
                                if (simulated <= pulledStack) {
                                    try (Transaction tx = TransferUtil.getTransaction()) {
                                        otherHandler.insert(ItemVariant.of(getStorage().getStackInSlot(otherSlot)), FabricUtil.extractSlot(getStorage(), otherSlot, pulledStack - simulated), tx);
                                        tx.commit();
                                    }
                                    break;
                                }
                            }
                        });
                    }
                    if (item.equals(FunctionalStorage.COLLECTOR_UPGRADE.get())) {
                        Direction direction = UpgradeItem.getDirection(stack);
                        AABB box = new AABB(pos.relative(direction));
                        for (ItemEntity entitiesOfClass : level.getEntitiesOfClass(ItemEntity.class, box)) {
                            ItemStack pulledStack = ItemHandlerHelper.copyStackWithSize(entitiesOfClass.getItem(), Math.min(entitiesOfClass.getItem().getCount(), 4));
                            if (pulledStack.isEmpty()) continue;
                            boolean hasWorked = false;
                            for (int ourSlot = 0; ourSlot < this.getStorage().getSlots(); ourSlot++) {
                                int simulated = (int) FabricUtil.insertSlotSimulated(getStorage(), ourSlot, pulledStack);
                                if (simulated != pulledStack.getCount()) {
                                    FabricUtil.insertSlot(getStorage(), ourSlot, ItemHandlerHelper.copyStackWithSize(entitiesOfClass.getItem(), pulledStack.getCount() - simulated));
                                    entitiesOfClass.getItem().shrink(pulledStack.getCount() - simulated);
                                    hasWorked = true;
                                    break;
                                }
                            }
                            if (hasWorked) break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public InteractionResult onSlotActivated(Player playerIn, InteractionHand hand, Direction facing, double hitX, double hitY, double hitZ, int slot) {
        ItemStack stack = playerIn.getItemInHand(hand);
        if (super.onActivated(playerIn, hand, facing, hitX, hitY, hitZ) == InteractionResult.SUCCESS) {
            return InteractionResult.SUCCESS;
        }
        if (slot != -1 && isServer()) {
            if (!stack.isEmpty() && FabricUtil.insertSlotSimulated(getStorage(), slot, stack) != stack.getCount()) {
                playerIn.setItemInHand(hand, FabricUtil.insertSlot(getStorage(), slot, stack));
                return InteractionResult.SUCCESS;
            } else if (System.currentTimeMillis() - INTERACTION_LOGGER.getOrDefault(playerIn.getUUID(), System.currentTimeMillis()) < 300) {
                for (ItemStack itemStack : playerIn.getInventory().items) {
                    if (!itemStack.isEmpty() && FabricUtil.insertSlotSimulated(getStorage(), slot, itemStack) != itemStack.getCount()) {
                        itemStack.setCount(FabricUtil.insertSlot(getStorage(), slot, itemStack.copy()).getCount());
                    }
                }
            }
            INTERACTION_LOGGER.put(playerIn.getUUID(), System.currentTimeMillis());
        }
        if (super.onSlotActivated(playerIn, hand, facing, hitX, hitY, hitZ, slot) == InteractionResult.SUCCESS) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    public abstract int getStorageSlotAmount();

    public void onClicked(Player playerIn, int slot) {
        if (isServer() && slot != -1) {
            HitResult rayTraceResult = RayTraceUtils.rayTraceSimple(this.level, playerIn, 16, 0);
            if (rayTraceResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockResult = (BlockHitResult) rayTraceResult;
                Direction facing = blockResult.getDirection();
                if (facing.equals(this.getFacingDirection())) {
                    try (Transaction tx = TransferUtil.getTransaction()) {
                        PlayerInventoryStorage.of(playerIn).offerOrDrop(ItemVariant.of(getStorage().getStackInSlot(slot)), FabricUtil.extractSlot(getStorage(), slot, playerIn.isShiftKeyDown() ? getStorage().getStackInSlot(slot).getMaxStackSize() : 1), tx);
                        tx.commit();
                    }
                }
            }
        }
    }

    public abstract SlotExposedStorage getStorage();

    public abstract int getBaseSize(int lost);

    @Override
    public InventoryComponent<ControllableDrawerTile<T>> getStorageUpgradesConstructor() {
        return new InventoryComponent<ControllableDrawerTile<T>>("storage_upgrades", 10, 70, getStorageSlotAmount()) {
            @Override
            public long extractSlot(int slot, ItemVariant variant, long amount, TransactionContext tx) {
                ItemStack stack = this.getStackInSlot(slot);
                if (stack.getItem() instanceof StorageUpgradeItem) {
                    int mult = 1;
                    for (int i = 0; i < getStorageUpgrades().getSlots(); i++) {
                        if (getStorageUpgrades().getStackInSlot(i).getItem() instanceof StorageUpgradeItem) {
                            if (i == slot) continue;
                            if (mult == 1)
                                mult = ((StorageUpgradeItem) getStorageUpgrades().getStackInSlot(i).getItem()).getStorageMultiplier();
                            else
                                mult *= ((StorageUpgradeItem) getStorageUpgrades().getStackInSlot(i).getItem()).getStorageMultiplier();
                        }
                    }
                    for (int i = 0; i < getStorage().getSlots(); i++) {
                        if (getStorage().getStackInSlot(i).isEmpty()) continue;
                        double stackSize = getStorage().getStackInSlot(i).getMaxStackSize() / 64D;
                        if ((int) Math.floor(Math.min(Integer.MAX_VALUE, getBaseSize(i) * (long) mult) * stackSize) < getStorage().getStackInSlot(i).getCount()) {
                            return 0;
                        }
                    }
                }
                return super.extractSlot(slot, variant, amount, tx);
            }

            @Override
            public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
                for(int slot = 0; slot < getSlots(); ++slot) {
                    ItemStack stack = this.getStackInSlot(slot);
                    if (stack.getItem() instanceof StorageUpgradeItem) {
                        int mult = 1;
                        for (int i = 0; i < getStorageUpgrades().getSlots(); i++) {
                            if (getStorageUpgrades().getStackInSlot(i).getItem() instanceof StorageUpgradeItem) {
                                if (i == slot) continue;
                                if (mult == 1)
                                    mult = ((StorageUpgradeItem) getStorageUpgrades().getStackInSlot(i).getItem()).getStorageMultiplier();
                                else
                                    mult *= ((StorageUpgradeItem) getStorageUpgrades().getStackInSlot(i).getItem()).getStorageMultiplier();
                            }
                        }
                        for (int i = 0; i < getStorage().getSlots(); i++) {
                            if (getStorage().getStackInSlot(i).isEmpty()) continue;
                            double stackSize = getStorage().getStackInSlot(i).getMaxStackSize() / 64D;
                            if ((int) Math.floor(Math.min(Integer.MAX_VALUE, getBaseSize(i) * (long) mult) * stackSize) < getStorage().getStackInSlot(i).getCount()) {
                                return 0;
                            }
                        }
                    }
                }
                return super.extract(resource, maxAmount, transaction);
            }
        }
                .setInputFilter((stack, integer) -> {
                    if (stack.getItem().equals(FunctionalStorage.STORAGE_UPGRADES.get(StorageUpgradeItem.StorageTier.IRON).get())) {
                        for (int i = 0; i < getStorage().getSlots(); i++) {
                            if (getStorage().getStackInSlot(i).getCount() > 64) {
                                return false;
                            }
                        }
                    }
                    return stack.getItem() instanceof UpgradeItem && ((UpgradeItem) stack.getItem()).getType() == UpgradeItem.Type.STORAGE;
                })
                .setOnSlotChanged((stack, integer) -> {
                    setNeedsUpgradeCache(true);
                })
                .setSlotLimit(1);
    }

    public boolean isEverythingEmpty() {
        for (int i = 0; i < getStorage().getSlots(); i++) {
            if (!getStorage().getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        for (int i = 0; i < getStorageUpgrades().getSlots(); i++) {
            if (!getStorageUpgrades().getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        for (int i = 0; i < getUtilityUpgrades().getSlots(); i++) {
            if (!getUtilityUpgrades().getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getTitleColor() {
        return ChatFormatting.DARK_GRAY.getColor();
    }

}

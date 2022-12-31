package com.buuz135.functionalstorage.block.tile;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.DrawerBlock;
import com.buuz135.functionalstorage.item.ConfigurationToolItem;
import com.buuz135.functionalstorage.item.LinkingToolItem;
import com.buuz135.functionalstorage.item.StorageUpgradeItem;
import com.buuz135.functionalstorage.item.UpgradeItem;
import com.buuz135.functionalstorage.util.FabricUtil;
import com.hrznstudio.titanium.annotation.Save;
import com.hrznstudio.titanium.block.BasicTileBlock;
import com.hrznstudio.titanium.block.tile.ActiveTile;
import com.hrznstudio.titanium.client.screen.addon.TextScreenAddon;
import com.hrznstudio.titanium.component.inventory.InventoryComponent;
import com.hrznstudio.titanium.util.RayTraceUtils;
import com.hrznstudio.titanium.util.TileUtil;
import io.github.fabricators_of_create.porting_lib.extensions.INBTSerializable;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlotExposedStorage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public abstract class ControllableDrawerTile<T extends ControllableDrawerTile<T>> extends ActiveTile<T> {

    private static HashMap<UUID, Long> INTERACTION_LOGGER = new HashMap<>();

    private boolean needsUpgradeCache = true;

    @Save
    private BlockPos controllerPos;
    @Save
    private InventoryComponent<ControllableDrawerTile<T>> storageUpgrades;
    @Save
    private InventoryComponent<ControllableDrawerTile<T>> utilityUpgrades;
    @Save
    private DrawerOptions drawerOptions;
    @Save
    private boolean hasDowngrade = false;
    @Save
    private boolean isCreative = false;
    @Save
    private boolean isVoid = false;
    @Save
    private int mult = 1;

    public ControllableDrawerTile(BasicTileBlock<T> base, BlockEntityType<T> entityType, BlockPos pos, BlockState state) {
        super(base, entityType, pos, state);
        this.drawerOptions = new DrawerOptions();
        this.storageUpgrades = new InventoryComponent<ControllableDrawerTile<T>>("storage_upgrades", 10, 70, getStorageSlotAmount()) {

            @Override
            public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
                for(int slot = 0; slot < getSlots(); ++slot) {
                    ItemStack stack = this.getStackInSlot(slot);
                    if (stack.getItem() instanceof StorageUpgradeItem) {
                        int mult = 1;
                        for (int i = 0; i < storageUpgrades.getSlots(); i++) {
                            if (storageUpgrades.getStackInSlot(i).getItem() instanceof StorageUpgradeItem) {
                                if (i == slot) continue;
                                if (mult == 1)
                                    mult = ((StorageUpgradeItem) storageUpgrades.getStackInSlot(i).getItem()).getStorageMultiplier();
                                else
                                    mult *= ((StorageUpgradeItem) storageUpgrades.getStackInSlot(i).getItem()).getStorageMultiplier();
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

            @Override
            public long extractSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext transaction) {
                ItemStack stack = this.getStackInSlot(slot);
                if (stack.getItem() instanceof StorageUpgradeItem) {
                    int mult = 1;
                    for (int i = 0; i < storageUpgrades.getSlots(); i++) {
                        if (storageUpgrades.getStackInSlot(i).getItem() instanceof StorageUpgradeItem) {
                            if (i == slot) continue;
                            if (mult == 1)
                                mult = ((StorageUpgradeItem) storageUpgrades.getStackInSlot(i).getItem()).getStorageMultiplier();
                            else
                                mult *= ((StorageUpgradeItem) storageUpgrades.getStackInSlot(i).getItem()).getStorageMultiplier();
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
                return super.extractSlot(slot, resource, maxAmount, transaction);
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
                    needsUpgradeCache = true;
                })
                .setSlotLimit(1);
        if (getStorageSlotAmount() > 0) {
            this.addInventory((InventoryComponent<T>) this.storageUpgrades);
        }
        this.addInventory((InventoryComponent<T>) (this.utilityUpgrades = new InventoryComponent<ControllableDrawerTile<T>>("utility_upgrades", 114, 70, 3)
                        .setInputFilter((stack, integer) -> stack.getItem() instanceof UpgradeItem && ((UpgradeItem) stack.getItem()).getType() == UpgradeItem.Type.UTILITY)
                        .setSlotLimit(1)
                        .setOnSlotChanged((itemStack, integer) -> {
                            needsUpgradeCache = true;
                            if (controllerPos != null && this.level.getBlockEntity(controllerPos) instanceof DrawerControllerTile controllerTile) {
                                controllerTile.getConnectedDrawers().rebuild();
                            }
                        })
                )
        );

    }

    @Override
    @Environment(EnvType.CLIENT)
    public void initClient() {
        super.initClient();
        if (getStorageSlotAmount() > 0) {
            addGuiAddonFactory(() -> new TextScreenAddon("Storage", 10, 59, false, ChatFormatting.DARK_GRAY.getColor()) {
                @Override
                public String getText() {
                    return Component.translatable("key.categories.storage").getString();
                }
            });
        }
        addGuiAddonFactory(() -> new TextScreenAddon("Utility", 114, 59, false, ChatFormatting.DARK_GRAY.getColor()) {
            @Override
            public String getText() {
                return Component.translatable("key.categories.utility").getString();
            }
        });
        addGuiAddonFactory(() -> new TextScreenAddon("key.categories.inventory", 8, 92, false, ChatFormatting.DARK_GRAY.getColor()) {
            @Override
            public String getText() {
                return Component.translatable("key.categories.inventory").getString();
            }
        });
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state, T blockEntity) {
        super.serverTick(level, pos, state, blockEntity);
        if (level.getGameTime() % 20 == 0) {
            for (int i = 0; i < this.utilityUpgrades.getSlots(); i++) {
                ItemStack stack = this.utilityUpgrades.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Item item = stack.getItem();
                    if (item.equals(FunctionalStorage.REDSTONE_UPGRADE.get())) {
                        level.updateNeighborsAt(this.getBlockPos(), this.getBasicTileBlock());
                        break;
                    }
                }
            }
        }
        if (level.getGameTime() % 4 == 0) {
            for (int i = 0; i < this.utilityUpgrades.getSlots(); i++) {
                ItemStack stack = this.utilityUpgrades.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Item item = stack.getItem();
                    if (item.equals(FunctionalStorage.PULLING_UPGRADE.get())) {
                        Direction direction = UpgradeItem.getDirection(stack);
                        Optional<BlockEntity> beOptional = TileUtil.getTileEntity(level, pos.relative(direction));
                        Storage<ItemVariant> iItemHandler = beOptional.isPresent() ? TransferUtil.getItemStorage(beOptional.get(), direction.getOpposite()) : TransferUtil.getItemStorage(level, pos.relative(direction), direction.getOpposite());
                        if (iItemHandler != null) {
                            for (StorageView<ItemVariant> view : iItemHandler) {
                                long pulledStack = TransferUtil.simulateExtractView(view, view.getResource(), 2);
                                if (pulledStack <= 0) continue;
                                boolean hasWorked = false;
                                for (int ourSlot = 0; ourSlot < this.getStorage().getSlots(); ourSlot++) {
                                    long simulated;
                                    try (Transaction tx = TransferUtil.getTransaction()) {
                                        simulated = getStorage().insertSlot(ourSlot, view.getResource(), pulledStack, tx);
                                    }

                                    if (simulated <= pulledStack) {
                                        try (Transaction tx = TransferUtil.getTransaction()) {
                                            getStorage().insertSlot(ourSlot, view.getResource(), view.extract(view.getResource(), pulledStack - simulated, tx), tx);
                                            tx.commit();
                                        }
                                        hasWorked = true;
                                        break;
                                    }
                                }
                                if (hasWorked) break;
                            }
                        }
                    }
                    if (item.equals(FunctionalStorage.PUSHING_UPGRADE.get())) {
                        Direction direction = UpgradeItem.getDirection(stack);
                        Optional<BlockEntity> beOptional = TileUtil.getTileEntity(level, pos.relative(direction));
                        Storage<ItemVariant> otherHandler = beOptional.isPresent() ? TransferUtil.getItemStorage(beOptional.get(), direction.getOpposite()) : TransferUtil.getItemStorage(level, pos.relative(direction), direction.getOpposite());
                        if (otherHandler != null) {
                            for (int otherSlot = 0; otherSlot < getStorage().getSlots(); otherSlot++) {
                                long pulledStack = FabricUtil.simulateExtractSlot(getStorage(), otherSlot, 2);
                                if (pulledStack <= 0) continue;
                                boolean hasWorked = false;
                                for (StorageView<ItemVariant> view : otherHandler) {
                                    long simulated = FabricUtil.simulateExtractView(view, ItemVariant.of(getStorage().getStackInSlot(otherSlot)), pulledStack);
                                    if (simulated <= pulledStack) {
                                        TransferUtil.insert(otherHandler, view.getResource(), FabricUtil.extractSlot(getStorage(), otherSlot, pulledStack - simulated));
                                        hasWorked = true;
                                        break;
                                    }
                                }
                                if (hasWorked) break;
                            }
                        }
                    }
                    if (item.equals(FunctionalStorage.COLLECTOR_UPGRADE.get())) {
                        Direction direction = UpgradeItem.getDirection(stack);
                        AABB box = new AABB(pos.relative(direction));
                        for (ItemEntity entitiesOfClass : level.getEntitiesOfClass(ItemEntity.class, box)) {
                            ItemStack pulledStack = ItemHandlerHelper.copyStackWithSize(entitiesOfClass.getItem(), Math.min(entitiesOfClass.getItem().getCount(), 4));
                            if (pulledStack.isEmpty()) continue;
                            boolean hasWorked = false;
                            for (int ourSlot = 0; ourSlot < this.getStorage().getSlots(); ourSlot++) {
                                long simulated;
                                try (Transaction tx = TransferUtil.getTransaction()) {
                                    simulated = getStorage().insertSlot(ourSlot, ItemVariant.of(pulledStack), pulledStack.getCount(), tx);
                                }
                                if (simulated != pulledStack.getCount()) {
                                    try (Transaction tx = TransferUtil.getTransaction()) {
                                        getStorage().insertSlot(ourSlot, ItemVariant.of(entitiesOfClass.getItem()), pulledStack.getCount() - simulated, tx);
                                        tx.commit();
                                    }
                                    entitiesOfClass.getItem().shrink((int) (pulledStack.getCount() - simulated));
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

    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public void setControllerPos(BlockPos controllerPos) {
        if (this.controllerPos != null) {
            TileUtil.getTileEntity(getLevel(), this.controllerPos, DrawerControllerTile.class).ifPresent(drawerControllerTile -> {
                drawerControllerTile.addConnectedDrawers(LinkingToolItem.ActionMode.REMOVE, getBlockPos());
            });
        }
        this.controllerPos = controllerPos;
    }

    public int getStorageMultiplier() {
        maybeCacheUpgrades();
        return mult;
    }

    public boolean isVoid() {
        maybeCacheUpgrades();
        return isVoid;
    }

    public boolean isCreative() {
        maybeCacheUpgrades();
        return isCreative;
    }

    public InteractionResult onSlotActivated(Player playerIn, InteractionHand hand, Direction facing, double hitX, double hitY, double hitZ, int slot) {
        ItemStack stack = playerIn.getItemInHand(hand);
        if (stack.getItem().equals(FunctionalStorage.CONFIGURATION_TOOL.get()) || stack.getItem().equals(FunctionalStorage.LINKING_TOOL.get()))
            return InteractionResult.PASS;
        if (!stack.isEmpty() && stack.getItem() instanceof UpgradeItem upgradeItem) {
            if (upgradeItem instanceof StorageUpgradeItem storageUpgradeItem) {
                InventoryComponent component = storageUpgrades;
                for (int i = 0; i < component.getSlots(); i++) {
                    if (component.getStackInSlot(i).isEmpty()) {
                        playerIn.setItemInHand(hand, FabricUtil.insertSlot(component, i, stack));
                        return InteractionResult.SUCCESS;
                    }
                }
                for (int i = 0; i < component.getSlots(); i++) {
                    if (!component.getStackInSlot(i).isEmpty() && component.getStackInSlot(i).getItem() instanceof StorageUpgradeItem instertedUpgrade && instertedUpgrade.getStorageMultiplier() < storageUpgradeItem.getStorageMultiplier()) {
                        try (Transaction tx = TransferUtil.getTransaction()) {
                            PlayerInventoryStorage.of(playerIn).offerOrDrop(ItemVariant.of(component.getStackInSlot(i)), component.getStackInSlot(i).getCount(), tx);
                            tx.commit();
                        }
                        component.setStackInSlot(i, ItemStack.EMPTY);
                        playerIn.setItemInHand(hand, FabricUtil.insertSlot(component, i, stack));
                        return InteractionResult.SUCCESS;
                    }
                }
            } else {
                InventoryComponent component = utilityUpgrades;
                for (int i = 0; i < component.getSlots(); i++) {
                    if (component.getStackInSlot(i).isEmpty()) {
                        playerIn.setItemInHand(hand, FabricUtil.insertSlot(component, i, stack));
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }
        if (super.onActivated(playerIn, hand, facing, hitX, hitY, hitZ) == InteractionResult.SUCCESS) {
            return InteractionResult.SUCCESS;
        }
        if (slot == -1) {
            openGui(playerIn);
        } else if (isServer()) {
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

    private void maybeCacheUpgrades() {
        if (needsUpgradeCache) {
            isCreative = false;
            hasDowngrade = false;
            mult = 1;
            for (int i = 0; i < storageUpgrades.getSlots(); i++) {
                Item upgrade = storageUpgrades.getStackInSlot(i).getItem();
                if (upgrade.equals(FunctionalStorage.STORAGE_UPGRADES.get(StorageUpgradeItem.StorageTier.IRON).get())) {
                    hasDowngrade = true;
                }
                if (upgrade.equals(FunctionalStorage.CREATIVE_UPGRADE.get())) {
                    isCreative = true;
                }
                if (upgrade instanceof StorageUpgradeItem) {
                    mult *= ((StorageUpgradeItem) upgrade).getStorageMultiplier();
                }
            }
            isVoid = false;
            for (int i = 0; i < utilityUpgrades.getSlots(); i++) {
                if (utilityUpgrades.getStackInSlot(i).getItem().equals(FunctionalStorage.VOID_UPGRADE.get())) {
                    isVoid = true;
                }
            }
            needsUpgradeCache = false;
        }
    }

    public boolean hasDowngrade() {
        maybeCacheUpgrades();
        return hasDowngrade;
    }

    public void toggleLocking() {
        setLocked(!this.isLocked());
    }

    public boolean isLocked() {
        return this.getBlockState().hasProperty(DrawerBlock.LOCKED) && this.getBlockState().getValue(DrawerBlock.LOCKED);
    }

    public void setLocked(boolean locked) {
        if (this.getBlockState().hasProperty(DrawerBlock.LOCKED)) {
            this.level.setBlock(this.getBlockPos(), this.getBlockState().setValue(DrawerBlock.LOCKED, locked), 3);
        }
    }

    public void toggleOption(ConfigurationToolItem.ConfigurationAction action) {
        this.drawerOptions.setActive(action, !this.drawerOptions.isActive(action));
        markForUpdate();
    }

    public DrawerOptions getDrawerOptions() {
        return drawerOptions;
    }


    public InventoryComponent<ControllableDrawerTile<T>> getUtilityUpgrades() {
        return utilityUpgrades;
    }

    public InventoryComponent<ControllableDrawerTile<T>> getStorageUpgrades() {
        return storageUpgrades;
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

    public static class DrawerOptions implements INBTSerializable<CompoundTag> {

        public HashMap<ConfigurationToolItem.ConfigurationAction, Boolean> options;

        public DrawerOptions() {
            this.options = new HashMap<>();
            this.options.put(ConfigurationToolItem.ConfigurationAction.TOGGLE_NUMBERS, true);
            this.options.put(ConfigurationToolItem.ConfigurationAction.TOGGLE_RENDER, true);
            this.options.put(ConfigurationToolItem.ConfigurationAction.TOGGLE_UPGRADES, true);
        }

        public boolean isActive(ConfigurationToolItem.ConfigurationAction configurationAction) {
            return options.getOrDefault(configurationAction, true);
        }

        public void setActive(ConfigurationToolItem.ConfigurationAction configurationAction, boolean active) {
            this.options.put(configurationAction, active);
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag compoundTag = new CompoundTag();
            for (ConfigurationToolItem.ConfigurationAction action : this.options.keySet()) {
                compoundTag.putBoolean(action.name(), this.options.get(action));
            }
            return compoundTag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            for (String allKey : nbt.getAllKeys()) {
                this.options.put(ConfigurationToolItem.ConfigurationAction.valueOf(allKey), nbt.getBoolean(allKey));
            }
        }
    }
}

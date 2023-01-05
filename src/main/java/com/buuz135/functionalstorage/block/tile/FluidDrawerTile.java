package com.buuz135.functionalstorage.block.tile;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.client.gui.FluidDrawerInfoGuiAddon;
import com.buuz135.functionalstorage.fluid.BigFluidHandler;
import com.buuz135.functionalstorage.item.StorageUpgradeItem;
import com.buuz135.functionalstorage.item.UpgradeItem;
import com.buuz135.functionalstorage.mixin.FlowingFluidAccessor;
import com.buuz135.functionalstorage.util.FabricUtil;
import com.hrznstudio.titanium.annotation.Save;
import com.hrznstudio.titanium.block.BasicTileBlock;
import com.hrznstudio.titanium.component.inventory.InventoryComponent;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.block.BucketPickupHandlerWrapper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.jetbrains.annotations.NotNull;

public class FluidDrawerTile extends ControllableDrawerTile<FluidDrawerTile> {

    @Save
    private BigFluidHandler fluidHandler;
    private FunctionalStorage.DrawerType type;

    public FluidDrawerTile(BasicTileBlock<FluidDrawerTile> base, BlockEntityType<FluidDrawerTile> blockEntityType, BlockPos pos, BlockState state, FunctionalStorage.DrawerType type) {
        super(base, blockEntityType, pos, state);
        this.type = type;
        this.fluidHandler = new BigFluidHandler(type.getSlots(), getTankCapacity(getStorageMultiplier())) {
            @Override
            public void onChange() {
                syncObject(fluidHandler);
            }

            @Override
            public boolean isDrawerLocked() {
                return isLocked();
            }

            @Override
            public boolean isDrawerVoid() {
                return isVoid();
            }

            @Override
            public boolean isDrawerCreative() {
                return isCreative();
            }
        };
    }

    private long getTankCapacity(int storageMultiplier) {
        long maxCap = ((type.getSlotAmount() / 64)) * FluidConstants.BUCKET * storageMultiplier;
        return maxCap;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void initClient() {
        super.initClient();
        var slotName = "";
        if (type.getSlots() == 2) {
            slotName = "_2";
        }
        if (type.getSlots() == 4) {
            slotName = "_4";
        }
        String finalSlotName = slotName;
        addGuiAddonFactory(() -> new FluidDrawerInfoGuiAddon(64, 16,
                new ResourceLocation(FunctionalStorage.MOD_ID, "textures/blocks/fluid_front" + finalSlotName + ".png"),
                type.getSlots(),
                type.getSlotPosition(),
                this::getFluidHandler,
                integer -> getFluidHandler().getTankCapacity(integer)
        ));
    }

    @Override
    public Storage<ItemVariant> getItemStorage(Direction side) {
        return null;
    }

    @Override
    public Storage<FluidVariant> getFluidStorage(Direction side) {
        return fluidHandler;
    }

    @Override
    public double getStorageDiv() {
        return 2;
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState stateOwn, FluidDrawerTile blockEntity) {
        super.serverTick(level, pos, stateOwn, blockEntity);
        if (level.getGameTime() % 4 == 0) {
            for (int i = 0; i < this.getUtilityUpgrades().getSlots(); i++) {
                var stack = this.getUtilityUpgrades().getStackInSlot(i);
                if (!stack.isEmpty()) {
                    var item = stack.getItem();
                    if (item.equals(FunctionalStorage.PUSHING_UPGRADE.get())) {
                        var direction = UpgradeItem.getDirection(stack);
                        FabricUtil.getStorage(FluidStorage.SIDED, level, pos.relative(direction), direction.getOpposite()).ifPresent(otherFluidHandler -> {
                            for (int tankId = 0; tankId < this.getFluidHandler().getTanks(); tankId++) {
                                var fluidTank = this.fluidHandler.getTankList()[tankId];
                                if (fluidTank.getFluid().isEmpty()) continue;
                                var extracted = TransferUtil.simulateExtractAnyFluid(fluidTank, 40500);
                                if (extracted.isEmpty()) continue;
                                var insertedAmount = TransferUtil.insertFluid(otherFluidHandler, extracted);
                                if (insertedAmount > 0) {
                                    TransferUtil.extractAnyFluid(fluidTank, insertedAmount);
                                    this.fluidHandler.onChange();
                                    break;
                                }
                            }
                        });
                    }
                    if (item.equals(FunctionalStorage.PULLING_UPGRADE.get())) {
                        var direction = UpgradeItem.getDirection(stack);
                        FabricUtil.getStorage(FluidStorage.SIDED, level, pos.relative(direction), direction.getOpposite()).ifPresent(otherFluidHandler -> {
                            for (int tankId = 0; tankId < this.getFluidHandler().getTanks(); tankId++) {
                                var fluidTank = this.fluidHandler.getTankList()[tankId];
                                var extracted = TransferUtil.simulateExtractAnyFluid(otherFluidHandler, 40500);
                                if (extracted.isEmpty()) continue;
                                var insertedAmount = TransferUtil.insertFluid(fluidTank, extracted);
                                if (insertedAmount > 0) {
                                    TransferUtil.extractAnyFluid(otherFluidHandler, insertedAmount);
                                    this.fluidHandler.onChange();
                                    break;
                                }
                            }
                        });
                    }
                    if (item.equals(FunctionalStorage.COLLECTOR_UPGRADE.get()) && level.getGameTime() % 20 == 0) {
                        var direction = UpgradeItem.getDirection(stack);
                        var fluidstate = this.level.getFluidState(this.getBlockPos().relative(direction));
                        if (!fluidstate.isEmpty() && fluidstate.isSource()) {
                            BlockState state = level.getBlockState(pos.relative(direction));
                            Block block = state.getBlock();
                            SingleSlotStorage<FluidVariant> targetFluidHandler = null;
                           if (block instanceof BucketPickup) {
                                targetFluidHandler = new BucketPickupHandlerWrapper((BucketPickup) block, level, pos.relative(direction));
                            }
                            if (targetFluidHandler != null) {
                                var drained = targetFluidHandler.simulateExtract(targetFluidHandler.getResource(), Integer.MAX_VALUE, null);
                                if (!(drained <= 0L)) {
                                    for (int tankId = 0; tankId < this.getFluidHandler().getTanks(); tankId++) {
                                        var fluidTank = this.fluidHandler.getTankList()[tankId];
                                        var insertedAmount = fluidTank.simulateInsert(targetFluidHandler.getResource(), drained, null);
                                        if (insertedAmount == drained) {
                                            TransferUtil.insert(fluidTank, targetFluidHandler.getResource(), drained);
                                            if (!(fluidstate.getType() instanceof FlowingFluid flowingFluid && ((FlowingFluidAccessor)flowingFluid).callCanConvertToSource())) {
                                                try (Transaction tx = TransferUtil.getTransaction()) {
                                                    targetFluidHandler.extract(StorageUtil.findExtractableResource(targetFluidHandler, tx), insertedAmount, tx);
                                                    tx.commit();
                                                }
                                            }
                                            this.fluidHandler.onChange();
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    @Override
    public InteractionResult onSlotActivated(Player playerIn, InteractionHand hand, Direction facing, double hitX, double hitY, double hitZ, int slot) {
        ItemStack stack = playerIn.getItemInHand(hand);
        if (stack.getItem().equals(FunctionalStorage.CONFIGURATION_TOOL.get()) || stack.getItem().equals(FunctionalStorage.LINKING_TOOL.get()))
            return InteractionResult.PASS;
        if (slot != -1 && !playerIn.getItemInHand(hand).isEmpty()) {
            return FluidStorageUtil.interactWithFluidStorage(this.fluidHandler.getTankList()[slot], playerIn, hand) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        return super.onSlotActivated(playerIn, hand, facing, hitX, hitY, hitZ, slot);
    }

    @Override
    public void onClicked(Player playerIn, int slot) {
        ItemStack stack = playerIn.getItemInHand(playerIn.getUsedItemHand());
        if (slot != -1 && !stack.isEmpty()) {
            FluidStorageUtil.interactWithFluidStorage(this.fluidHandler.getTankList()[slot], playerIn, playerIn.getUsedItemHand());
        }
    }

    @NotNull
    @Override
    public FluidDrawerTile getSelf() {
        return this;
    }

    public FunctionalStorage.DrawerType getDrawerType() {
        return type;
    }

    @Override
    public int getStorageSlotAmount() {
        return 4;
    }

    @Override
    public int getBaseSize(int lost) {
        return type.getSlotAmount();
    }

    public BigFluidHandler getFluidHandler() {
        return fluidHandler;
    }

    @Override
    public void setLocked(boolean locked) {
        super.setLocked(locked);
        this.fluidHandler.lockHandler();
        syncObject(this.fluidHandler);
    }

    public boolean isEverythingEmpty() {
        for (int i = 0; i < getFluidHandler().getTanks(); i++) {
            if (!getFluidHandler().getFluidInTank(i).isEmpty()) {
                return false;
            }
        }
        if (isLocked()) return false;
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
    public InventoryComponent<ControllableDrawerTile<FluidDrawerTile>> getStorageUpgradesConstructor() {
        return new InventoryComponent<ControllableDrawerTile<FluidDrawerTile>>("storage_upgrades", 10, 70, getStorageSlotAmount()) {
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
                    for (int i = 0; i < getFluidHandler().getTanks(); i++) {
                        if (getFluidHandler().getFluidInTank(i).isEmpty()) continue;
                        if (getFluidHandler().getFluidInTank(i).getAmount() > getTankCapacity(mult)) {
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
                                var calculated = ((StorageUpgradeItem) getStorageUpgrades().getStackInSlot(i).getItem()).getStorageMultiplier() / getStorageDiv();
                            if (mult == 1)
                                mult = (int) calculated;
                                else
                                    mult *= calculated;
                            }
                        }
                        for (int i = 0; i < getFluidHandler().getTanks(); i++) {
                            if (getFluidHandler().getFluidInTank(i).isEmpty()) continue;
                            if (getFluidHandler().getFluidInTank(i).getAmount() > getTankCapacity(mult)) {
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
                        return false;
                    }
                    return stack.getItem() instanceof UpgradeItem && ((UpgradeItem) stack.getItem()).getType() == UpgradeItem.Type.STORAGE;
                })
                .setOnSlotChanged((stack, integer) -> {
                    setNeedsUpgradeCache(true);
                    this.fluidHandler.setCapacity(getTankCapacity(getStorageMultiplier()));
                    syncObject(this.fluidHandler);
                })
                .setSlotLimit(1);
    }


}

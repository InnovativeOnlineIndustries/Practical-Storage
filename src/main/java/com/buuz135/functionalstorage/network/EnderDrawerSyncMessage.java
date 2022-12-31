package com.buuz135.functionalstorage.network;

import com.buuz135.functionalstorage.inventory.EnderInventoryHandler;
import com.buuz135.functionalstorage.world.EnderSavedData;
import com.hrznstudio.titanium.network.CompoundSerializableDataHandler;
import com.hrznstudio.titanium.network.Message;
import me.pepperbell.simplenetworking.SimpleChannel;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketListener;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;

public class EnderDrawerSyncMessage extends Message {

    static {
        CompoundSerializableDataHandler.map(EnderInventoryHandler.class, buf -> {
            EnderInventoryHandler handler = new EnderInventoryHandler(buf.readUtf(), EnderSavedData.CLIENT);
            handler.deserializeNBT(buf.readNbt());
            return handler;
        }, (buf, handler1) -> {
            buf.writeUtf(handler1.getFrequency());
            buf.writeNbt(handler1.serializeNBT());
        });
    }


    public String frequency;
    public EnderInventoryHandler handler;

    public EnderDrawerSyncMessage(String frequency, EnderInventoryHandler handler) {
        this.frequency = frequency;
        this.handler = handler;
    }

    public EnderDrawerSyncMessage() {
    }

    @Override
    protected void handleMessage(Executor executor, @Nullable Player sender, PacketListener packetListener, PacketSender packetSender, SimpleChannel channel) {
        executor.execute(() -> {
            EnderSavedData.getInstance(Minecraft.getInstance().level).setFrenquency(frequency, handler);
        });
    }
}

package com.buuz135.functionalstorage.mixin;

import net.minecraft.world.level.material.FlowingFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FlowingFluid.class)
public interface FlowingFluidAccessor {
    @Invoker
    boolean callCanConvertToSource();
}

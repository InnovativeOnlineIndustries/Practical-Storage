package com.buuz135.functionalstorage.compat;

import com.buuz135.functionalstorage.compat.top.FunctionalDrawerProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ITheOneProbePlugin;

public class TOPPlugin implements ITheOneProbePlugin {
    @Override
    public void onLoad(ITheOneProbe api) {
        FunctionalDrawerProvider.REGISTER.apply(api);
    }

}

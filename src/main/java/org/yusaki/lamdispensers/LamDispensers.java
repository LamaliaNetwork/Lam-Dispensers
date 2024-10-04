package org.yusaki.lamdispensers;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.yusaki.lib.YskLib;

public final class LamDispensers extends SimplePlugin {

    private YskLib yskLib;
    @Getter
    private YskLibWrapper wrapper;

    @Override
    public void onPluginStart() {
        yskLib = (YskLib) Bukkit.getPluginManager().getPlugin("YskLib");
        wrapper = new YskLibWrapper(this, yskLib);

        // Register the new DispenserPlacementHandler
        DispenserPlacementHandler handler = new DispenserPlacementHandler(this);
        registerEvents(handler);


        wrapper.logDebug("LamDispensers enabled!");
    }

    @Override
    public void onPluginStop() {
        wrapper.logDebug("LamDispensers disabled!");
    }

}

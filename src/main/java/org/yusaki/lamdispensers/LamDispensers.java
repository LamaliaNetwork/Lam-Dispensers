package org.yusaki.lamdispensers;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.yusaki.lib.YskLib;

public final class LamDispensers extends SimplePlugin {

    private YskLib yskLib;
    @Getter
    private YskLibWrapper wrapper;
    private DispenserPlacementHandler handler;

    @Override
    public void onPluginStart() {
        yskLib = (YskLib) Bukkit.getPluginManager().getPlugin("YskLib");
        wrapper = new YskLibWrapper(this, yskLib);

        handler = new DispenserPlacementHandler(this);
        getServer().getPluginManager().registerEvents(handler, this);

        // Initialize existing dispensers in loaded chunks
        getServer().getScheduler().runTask(this, () -> {
            handler.initializeExistingDispensers();
        });


        wrapper.logDebug("LamDispensers enabled!");
    }

    @Override
    public void onPluginStop() {
        wrapper.logDebug("LamDispensers disabled!");
    }

}

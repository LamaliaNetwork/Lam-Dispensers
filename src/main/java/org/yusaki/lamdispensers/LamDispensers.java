package org.yusaki.lamdispensers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.yusaki.lib.YskLib;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class LamDispensers extends SimplePlugin {

    private YskLib yskLib;
    private YskLibWrapper wrapper;
    private final Random random = new Random();


    @Override
    public void onPluginStart() {
        yskLib = (YskLib) Bukkit.getPluginManager().getPlugin("YskLib");
        wrapper = new YskLibWrapper(this, yskLib);
        getServer().getPluginManager().registerEvents(this, this);

        wrapper.logDebug("LamDispensers enabled!");
    }

    @Override
    public void onPluginStop() {
        wrapper.logDebug("LamDispensers disabled!");
    }

    @EventHandler
    public void onDispense(BlockDispenseEvent event) {
        Block dispenserBlock = event.getBlock();
        if (!(dispenserBlock.getState() instanceof Dispenser)) return;

        Dispenser dispenser = (Dispenser) dispenserBlock.getState();
        if (!(dispenserBlock.getBlockData() instanceof Directional)) return;

        Directional directional = (Directional) dispenserBlock.getBlockData();
        BlockFace facing = directional.getFacing();
        Block frontBlock = dispenserBlock.getRelative(facing);

        if (frontBlock.getType() != Material.AIR) return;

        event.setCancelled(true);

        Location loc = dispenserBlock.getLocation();
        scheduleFoliaCompatibleTask(loc, () -> {
            // Re-check conditions
            if (frontBlock.getType() != Material.AIR) return;

            ItemStack selectedItem = getRandomItemFromDispenser(dispenser);
            if (selectedItem != null && selectedItem.getType().isBlock() && selectedItem.getType().isSolid()) {
                if (removeItem(dispenser, selectedItem)) {
                    frontBlock.setType(selectedItem.getType());
                    getLogger().info("Dispenser placed a " + selectedItem.getType() + " block.");
                }
            }
        });
    }

    private ItemStack getRandomItemFromDispenser(Dispenser dispenser) {
        ItemStack[] contents = dispenser.getInventory().getContents();
        int[] filledSlots = new int[9];
        int filledCount = 0;

        // Find all slots with items
        for (int i = 0; i < 9; i++) {
            if (contents[i] != null && !contents[i].getType().isAir()) {
                filledSlots[filledCount++] = i;
            }
        }

        // If no items, return null
        if (filledCount == 0) return null;

        // Pick a random filled slot
        int selectedSlot = filledSlots[random.nextInt(filledCount)];
        return contents[selectedSlot];
    }

    private boolean removeItem(Dispenser dispenser, ItemStack item) {
        ItemStack[] contents = dispenser.getInventory().getContents();
        int slot = dispenser.getInventory().first(item);
        if (slot >= 0) {
            if (contents[slot].getAmount() > 1) {
                contents[slot].setAmount(contents[slot].getAmount() - 1);
            } else {
                contents[slot] = null;
            }
            dispenser.getInventory().setContents(contents);
            return true;
        }
        return false;
    }

    private void scheduleFoliaCompatibleTask(Location location, Runnable task) {
        try {
            // Try to schedule using Folia's region-based scheduler
            getServer().getRegionScheduler().execute(this, location, task);
        } catch (NoSuchMethodError e) {
            // Fallback to Bukkit scheduler if Folia is not available
            getServer().getScheduler().runTask(this, task);
        }
    }
}

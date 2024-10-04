package org.yusaki.lamdispensers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DispenserToolHandler {

    private final JavaPlugin plugin;
    private final Set<Material> tools = new HashSet<>();

    public DispenserToolHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        initializeTools();
    }

    private void initializeTools() {
        tools.addAll(Arrays.asList(
                Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
                Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
                Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
                Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
                Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
                Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL
        ));
    }

    public boolean isTool(Material material) {
        return tools.contains(material);
    }

    public void handleToolDispense(Dispenser dispenser, Block targetBlock, ItemStack tool) {
        if (targetBlock.getType().isAir()) return;

        float breakSpeed = calculateBreakSpeed(tool, targetBlock);
        int breakTicks = Math.max(1, Math.round(1 / breakSpeed * 20)); // Convert to ticks, minimum 1 tick

        scheduleFoliaCompatibleTask(targetBlock.getLocation(), () -> {
            startBlockBreaking(dispenser, targetBlock, tool, breakTicks);
        });
    }

    private float calculateBreakSpeed(ItemStack tool, Block block) {
        float baseSpeed = tool.getType().getMaxDurability() / 1000f; // Rough estimate
        int efficiencyLevel = tool.getEnchantmentLevel(Enchantment.DIG_SPEED);
        float efficiencyBonus = (efficiencyLevel > 0) ? (efficiencyLevel * efficiencyLevel + 1) : 0;

        float speed = baseSpeed + efficiencyBonus;

        // TODO: Add more precise calculations based on tool-block relationships

        return speed;
    }

    private void startBlockBreaking(Dispenser dispenser, Block block, ItemStack tool, int breakTicks) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= breakTicks) {
                    // Break the block
                    BlockBreakEvent breakEvent = new BlockBreakEvent(block, null);
                    Bukkit.getPluginManager().callEvent(breakEvent);
                    if (!breakEvent.isCancelled()) {
                        block.breakNaturally(tool);
                        damageToolInDispenser(dispenser, tool);
                    }
                    this.cancel();
                } else {
                    // Show block break animation
                    block.getWorld().sendBlockDamage(block.getLocation(), (float) ticks / breakTicks, dispenser.getLocation());
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void damageToolInDispenser(Dispenser dispenser, ItemStack tool) {
        ItemStack[] contents = dispenser.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].isSimilar(tool)) {
                if (tool.getDurability() >= tool.getType().getMaxDurability()) {
                    contents[i] = null;
                } else {
                    tool.setDurability((short) (tool.getDurability() + 1));
                    contents[i] = tool;
                }
                dispenser.getInventory().setContents(contents);
                break;
            }
        }
    }

    private void scheduleFoliaCompatibleTask(Location location, Runnable task) {
        try {
            plugin.getServer().getRegionScheduler().execute(plugin, location, task);
        } catch (NoSuchMethodError e) {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }
}

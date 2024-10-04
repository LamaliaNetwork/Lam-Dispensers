package org.yusaki.lamdispensers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class DispenserPlacementHandler implements Listener {

    private final LamDispensers plugin;
    private final Random random = new Random();
    private final Set<Material> replaceable = new HashSet<>();
    private final Set<Material> placeableBlocks = new HashSet<>();
    private final Map<Material, Set<Material>> toolEffectiveness = new HashMap<>();
    private final Map<Location, MiningTask> activeMiningTasks = new HashMap<>();
    private final Set<Material> tools = EnumSet.of(
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
            Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
            Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
            Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL
    );

    public DispenserPlacementHandler(LamDispensers plugin) {
        this.plugin = plugin;
        initializeReplaceableBlocks();
        initializePlaceableBlocks();
        initializeToolEffectiveness();
    }

    // Init

    private void initializeReplaceableBlocks() {
        replaceable.add(Material.AIR);
        replaceable.add(Material.WATER);
        replaceable.add(Material.LAVA);
    }


    private void initializePlaceableBlocks() {
        // Add all solid blocks
        for (Material material : Material.values()) {
            if (material.isBlock() && material.isSolid()) {
                placeableBlocks.add(material);
            }
        }

        // Remove TNT and Shulker Boxes
        placeableBlocks.remove(Material.TNT);
        for (Material material : Material.values()) {
            if (material.name().endsWith("SHULKER_BOX")) {
                placeableBlocks.remove(material);
            }
        }

        // Add specific transparent blocks
        Material[] transparentBlocks = {
                // Carpets
                Material.WHITE_CARPET, Material.ORANGE_CARPET, Material.MAGENTA_CARPET,
                Material.LIGHT_BLUE_CARPET, Material.YELLOW_CARPET, Material.LIME_CARPET,
                Material.PINK_CARPET, Material.GRAY_CARPET, Material.LIGHT_GRAY_CARPET,
                Material.CYAN_CARPET, Material.PURPLE_CARPET, Material.BLUE_CARPET,
                Material.BROWN_CARPET, Material.GREEN_CARPET, Material.RED_CARPET,
                Material.BLACK_CARPET,

                // Rails
                Material.RAIL, Material.POWERED_RAIL, Material.DETECTOR_RAIL,
                Material.ACTIVATOR_RAIL,

                // Saplings
                Material.OAK_SAPLING, Material.SPRUCE_SAPLING, Material.BIRCH_SAPLING,
                Material.JUNGLE_SAPLING, Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING,
                Material.MANGROVE_PROPAGULE,

                // Other common transparent blocks
                Material.TORCH, Material.REDSTONE_TORCH, Material.LEVER, Material.STONE_BUTTON,
                Material.OAK_BUTTON, Material.SPRUCE_BUTTON, Material.BIRCH_BUTTON,
                Material.JUNGLE_BUTTON, Material.ACACIA_BUTTON, Material.DARK_OAK_BUTTON,
                Material.CRIMSON_BUTTON, Material.WARPED_BUTTON, Material.REPEATER,
                Material.COMPARATOR, Material.REDSTONE_WIRE
        };

        placeableBlocks.addAll(Set.of(transparentBlocks));
    }

    private void initializeToolEffectiveness() {
        Set<Material> pickaxeEffective = new HashSet<>();
        Set<Material> axeEffective = new HashSet<>();
        Set<Material> shovelEffective = new HashSet<>();

        for (Material material : Material.values()) {
            if (material.isBlock()) {
                if (material.name().contains("STONE") || material.name().contains("ORE") ||
                        material == Material.OBSIDIAN || material == Material.COBBLESTONE) {
                    pickaxeEffective.add(material);
                } else if (material.name().contains("LOG") || material.name().contains("WOOD") ||
                        material == Material.CHEST) {
                    axeEffective.add(material);
                } else if (material == Material.DIRT || material == Material.GRASS_BLOCK ||
                        material == Material.SAND || material == Material.GRAVEL) {
                    shovelEffective.add(material);
                }
            }
        }

        for (Material tool : tools) {
            if (tool.name().endsWith("PICKAXE")) {
                toolEffectiveness.put(tool, pickaxeEffective);
            } else if (tool.name().endsWith("AXE")) {
                toolEffectiveness.put(tool, axeEffective);
            } else if (tool.name().endsWith("SHOVEL")) {
                toolEffectiveness.put(tool, shovelEffective);
            }
        }
    }

    // Main Event Handler

    @EventHandler
    public void onDispense(BlockDispenseEvent event) {
        Block dispenserBlock = event.getBlock();
        if (!(dispenserBlock.getState() instanceof Dispenser)) return;

        Dispenser dispenser = (Dispenser) dispenserBlock.getState();
        if (!(dispenserBlock.getBlockData() instanceof Directional)) return;

        Directional directional = (Directional) dispenserBlock.getBlockData();
        BlockFace facing = directional.getFacing();
        Block frontBlock = dispenserBlock.getRelative(facing);

        ItemStack dispensedItem = event.getItem();

        if (tools.contains(dispensedItem.getType())) {
            event.setCancelled(true);
            plugin.getLogger().info("Tool dispensed: " + dispensedItem.getType());
            startOrResumeMining(dispenser, facing);
        } else if (replaceable.contains(frontBlock.getType()) && placeableBlocks.contains(dispensedItem.getType())) {
            event.setCancelled(true);
            handlePlacement(dispenser, frontBlock, dispensedItem);
        }
    }


    private void startOrResumeMining(Dispenser dispenser, BlockFace facing) {
        Location dispenserLoc = dispenser.getLocation();
        MiningTask existingTask = activeMiningTasks.get(dispenserLoc);

        if (existingTask != null) {
            plugin.getLogger().info("Mining task already exists for dispenser at " + dispenserLoc);
        } else {
            plugin.getLogger().info("Starting new mining task for dispenser at " + dispenserLoc);
            MiningTask newTask = new MiningTask(dispenser, facing);
            activeMiningTasks.put(dispenserLoc, newTask);
            scheduleFoliaCompatibleTask(dispenserLoc, newTask);
        }
    }

    private class MiningTask implements Runnable {
        private final Dispenser dispenser;
        private final BlockFace facing;
        private ItemStack currentTool;
        private Block targetBlock;
        private int progress = 0;
        private int breakTime;

        public MiningTask(Dispenser dispenser, BlockFace facing) {
            this.dispenser = dispenser;
            this.facing = facing;
            this.targetBlock = dispenser.getBlock().getRelative(facing);
        }

        @Override
        public void run() {
            if (!dispenser.getBlock().isBlockPowered()) {
                plugin.getLogger().info("Dispenser no longer powered. Pausing mining.");
                clearBlockDamage();
                scheduleFoliaCompatibleTask(dispenser.getLocation(), this, 20L); // Check again in 1 second
                return;
            }

            ItemStack newTool = findToolInDispenser();
            if (newTool == null) {
                plugin.getLogger().info("No valid tool found in dispenser. Pausing mining.");
                clearBlockDamage();
                scheduleFoliaCompatibleTask(dispenser.getLocation(), this, 20L); // Check again in 1 second
                return;
            }

            if (!newTool.equals(currentTool)) {
                currentTool = newTool;
                progress = 0; // Reset progress when tool changes
                plugin.getLogger().info("Tool changed to: " + currentTool.getType());
            }

            targetBlock = dispenser.getBlock().getRelative(facing);

            if (targetBlock.getType() == Material.AIR || !canMine(currentTool, targetBlock)) {
                plugin.getLogger().info("No minable block found. Waiting for block to appear.");
                clearBlockDamage();
                progress = 0;
                scheduleFoliaCompatibleTask(dispenser.getLocation(), this, 20L); // Check again in 1 second
                return;
            }

            if (progress == 0) {
                breakTime = calculateBreakTime(currentTool, targetBlock);
                plugin.getLogger().info("New minable block detected. Break time: " + breakTime + " ticks");
            }

            progress++;
            int stage = (int) ((float) progress / breakTime * 10);
            showBlockDamage(stage);

            plugin.getLogger().info("Mining progress: " + progress + "/" + breakTime + " ticks");

            if (progress >= breakTime) {
                plugin.getLogger().info("Block mined. Breaking block.");
                targetBlock.breakNaturally(currentTool);
                plugin.getLogger().info("Dispenser mined a " + targetBlock.getType() + " block with " + currentTool.getType());
                progress = 0;
            }

            scheduleFoliaCompatibleTask(dispenser.getLocation(), this, 1L); // Continue mining next tick
        }

        private ItemStack findToolInDispenser() {
            for (ItemStack item : dispenser.getInventory().getContents()) {
                if (item != null && tools.contains(item.getType())) {
                    return item;
                }
            }
            return null;
        }

        private void clearBlockDamage() {
            for (Player player : targetBlock.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(targetBlock.getLocation()) < 65536) {
                    player.sendBlockDamage(targetBlock.getLocation(), 0);
                }
            }
        }

        private void showBlockDamage(int stage) {
            float damage = Math.min(1.0f, Math.max(0.0f, stage / 10.0f));
            for (Player player : targetBlock.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(targetBlock.getLocation()) < 65536) {
                    player.sendBlockDamage(targetBlock.getLocation(), damage);
                }
            }
        }
    }

    private boolean canMine(ItemStack tool, Block block) {
        Set<Material> effectiveBlocks = toolEffectiveness.get(tool.getType());
        return effectiveBlocks != null && effectiveBlocks.contains(block.getType());
    }

    private int calculateBreakTime(ItemStack tool, Block block) {
        float hardness = block.getType().getHardness();
        float toolSpeed = getToolSpeed(tool);
        // Adjust this formula as needed to get the desired mining speed
        return Math.max(1, Math.round((hardness * 30) / toolSpeed));
    }

    private float getToolSpeed(ItemStack tool) {
        String toolMaterial = tool.getType().name().split("_")[0];
        String toolType = tool.getType().name().split("_")[1];
        float baseSpeed;

        switch (toolType) {
            case "PICKAXE":
                baseSpeed = 1.5f;
                break;
            case "AXE":
                baseSpeed = 1.0f;
                break;
            case "SHOVEL":
                baseSpeed = 1.0f;
                break;
            default:
                return 1.0f;
        }

        float materialMultiplier;
        switch (toolMaterial) {
            case "WOODEN":
                materialMultiplier = 2.0f;
                break;
            case "STONE":
                materialMultiplier = 4.0f;
                break;
            case "IRON":
                materialMultiplier = 6.0f;
                break;
            case "DIAMOND":
                materialMultiplier = 8.0f;
                break;
            case "NETHERITE":
                materialMultiplier = 9.0f;
                break;
            case "GOLDEN":
                materialMultiplier = 12.0f;
                break;
            default:
                materialMultiplier = 1.0f;
        }

        return baseSpeed * materialMultiplier;
    }


    // Block Placement Section
    private void handlePlacement(Dispenser dispenser, Block frontBlock, ItemStack item) {
        Location loc = dispenser.getLocation();
        scheduleFoliaCompatibleTask(loc, () -> {
            // Re-check conditions
            if (!replaceable.contains(frontBlock.getType())) return;

            ItemStack selectedItem = getRandomItemFromDispenser(dispenser);
            if (selectedItem != null && placeableBlocks.contains(selectedItem.getType())) {
                if (removeItem(dispenser, selectedItem)) {
                    Material originalType = frontBlock.getType();
                    frontBlock.setType(selectedItem.getType());
                    plugin.getLogger().info("Dispenser placed a " + selectedItem.getType() + " block, replacing " + originalType);
                }
            }
        });
    }

    private ItemStack getRandomItemFromDispenser(Dispenser dispenser) {
        ItemStack[] contents = dispenser.getInventory().getContents();
        int[] filledSlots = new int[9];
        int filledCount = 0;

        for (int i = 0; i < 9; i++) {
            if (contents[i] != null && !contents[i].getType().isAir()) {
                filledSlots[filledCount++] = i;
            }
        }

        if (filledCount == 0) return null;

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

    private void scheduleFoliaCompatibleTask(Location location, Runnable task, long delay) {
        try {
            plugin.getServer().getRegionScheduler().runDelayed(plugin, location, (scheduledTask) -> task.run(), delay);
        } catch (NoSuchMethodError e) {
            plugin.getServer().getScheduler().runTaskLater(plugin, task, delay);
        }
    }

    private void scheduleFoliaCompatibleTask(Location location, Runnable task) {
        scheduleFoliaCompatibleTask(location, task, 1L);
    }






}



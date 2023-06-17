package me.viciscat.mineralcontest;

import me.viciscat.mineralcontest.game.GameHandler;
import me.viciscat.mineralcontest.ui.ClassSelectUI;
import me.viciscat.mineralcontest.ui.TeamSelectUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.BoundingBox;

import java.util.Map;
import java.util.Objects;

public class MineralListener implements Listener {

    Map<World, GameHandler> map = MineralContest.instance.gameHandlerMap;
    String[] classes = new String[]{"agile", "worker", "robust", "warrior", "miner"};

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (!map.containsKey(entity.getWorld())) return;
        BoundingBox boundingBox = new BoundingBox(-80, 0, -80, 80, 255, 80);
        CreatureSpawnEvent.SpawnReason spawnReason = event.getSpawnReason();
        if (spawnReason.equals(CreatureSpawnEvent.SpawnReason.NATURAL) && boundingBox.contains(entity.getLocation().toVector())) {
            event.setCancelled(true);

        }
    }

    @EventHandler
    public void onCloseInventory(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return; // Is it a player?
        if (!map.containsKey(player.getWorld())) return; // Is it in a Mineral Contest instance?

        Inventory inventory = event.getInventory();
        GameHandler gameHandler = map.get(player.getWorld());

        if (!inventory.getType().equals(InventoryType.ENDER_CHEST)) return; // Is it an EC ?
        if (inventory.getLocation() == null) return;
        if (gameHandler.getTeamID(inventory.getLocation()) == -1) return;
        int chestTeamID = gameHandler.getTeamID(inventory.getLocation());
        int playerTeamID = gameHandler.getTeamID(player);
        Inventory playerInventory = player.getInventory();
        if (playerTeamID == -1 || chestTeamID != playerTeamID) {
            for (ItemStack item : inventory.getStorageContents()) {
                if (item != null) { playerInventory.addItem(item); }
            }

            inventory.clear();
            player.sendMessage(Component.text("Wrong team chest >:( How did you get in here?"));
            return;
        }
        int finalScore = 0;
        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack == null) { finalScore += 0; } else {
                finalScore += gameHandler.scoreMap.getOrDefault(itemStack.getType(), 0) * itemStack.getAmount();
                inventory.remove(itemStack);
            }
        }
        MineralTeam team =  gameHandler.getTeam(playerTeamID);
        finalScore *= team.getScoreMultiplier();
        team.addScore(finalScore);

        Location location = inventory.getLocation();
        player.sendMessage(Component.text("You closed an enderchest! Is it one of the castle's one? Who knows cuz this hasn't been implemented yet!!"));
        player.sendMessage(Component.text(location.getX() + " " + location.getY() + " " + location.getZ()));


    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!map.containsKey(player.getWorld())) return;
        if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)) return;
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        GameHandler gameHandler = map.get(player.getWorld());
        if (gameHandler.gamePhase == GameHandler.Phase.GAME) return;
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType().equals(compass.getType())) {
            event.setCancelled(true);
            TeamSelectUI.openUI(player, gameHandler);
        } else if (heldItem.getType().equals(sword.getType())) {
            event.setCancelled(true);
            ClassSelectUI.openUI(player, gameHandler);

        }

    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return; // Is it a player?
        if (!map.containsKey(player.getWorld())) return;
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();
        if (holder == null) return;
        if (holder instanceof TeamSelectUI.Holder) {
            // TEAM SELECTION
            GameHandler gameHandler = map.get(player.getWorld());
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot >= 10 && slot <= 16 && slot%2==0) {
                int teamID = (slot - 10) / 2;
                // If player already in the clicked team
                if (gameHandler.getTeam(teamID).playerInTeam(player)) {
                    player.sendMessage(Component.text("You are already in this team buckaroo!"));
                    return;
                }
                // Leave previous team
                int oldTeamID = gameHandler.getTeamID(player);
                if (oldTeamID != -1) { gameHandler.getTeam(oldTeamID).removePlayer(player); }
                // Send message and join new team
                player.sendMessage(Component.text("You joined the ")
                        .append(Component.text(TeamSelectUI.teamColors[teamID], TeamSelectUI.textColors[teamID]))
                        .append(Component.text(" team!")));
                gameHandler.getTeam(teamID).addPlayer(player);

            }
        } else if (holder instanceof ClassSelectUI.Holder) {
            // CLASS SELECTION
            GameHandler gameHandler = map.get(player.getWorld());
            event.setCancelled(true);
            int slot = event.getRawSlot();
            int classID = slot - 11;
            if (classID > 4) return;
            gameHandler.setClass(player, classes[classID]);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        if (!map.containsKey(player.getWorld())) return;
        GameHandler gameHandler = map.get(player.getWorld());
        if (!(Objects.equals(gameHandler.getPlayerClass(player), "warrior"))) return;
        event.setDamage(event.getDamage() * 1.25);

    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (!map.containsKey(player.getWorld())) return;
        GameHandler gameHandler = map.get(player.getWorld());
       switch (gameHandler.getPlayerClass(player)) {
            case "robust" -> event.setDamage(event.getDamage() * 0.85);
            case "agile" -> {
               if (event.getCause().equals(EntityDamageEvent.DamageCause.FALL)) event.setCancelled(true);
           }
        }


    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (!map.containsKey(player.getWorld())) {
            AttributeInstance speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            assert speedAttr != null;
            speedAttr.removeModifier(new AttributeModifier("mineralcontest_agile", 0.2, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
            return;
        }
        GameHandler gameHandler = map.get(player.getWorld());
        // Create scoreboard if player has no scoreboard
        gameHandler.playerScoreboards.computeIfAbsent(player.getUniqueId(), k -> {
            Scoreboard newScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective newObjective = newScoreboard.registerNewObjective("mineral_contest_gui", Criteria.DUMMY, Component.text("Mineral Contest").decoration(TextDecoration.BOLD, true).color(NamedTextColor.DARK_AQUA));
            newObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
            player.setScoreboard(newScoreboard);
            return newScoreboard;
        });
        // Update scoreboard
        gameHandler.resetPlayerScoreboard(player);
        if (gameHandler.gamePhase == GameHandler.Phase.PREGAME) {
            Inventory playerInventory = player.getInventory();
            playerInventory.clear();
            ItemStack compass = new ItemStack(Material.COMPASS);
            ItemMeta compassMeta = compass.getItemMeta();
            compassMeta.displayName(Component.text("Right click to select your team!").decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
            compass.setItemMeta(compassMeta);
            playerInventory.setItem(8, compass);

            gameHandler.pregameTeam.addPlayer(player);
        }
    }
    @EventHandler
    public void onBlockDropItem(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        if (!player.getGameMode().equals(GameMode.CREATIVE)) return;
        if (!map.containsKey(player.getWorld())) return;
        GameHandler gameHandler = map.get(player.getWorld());
        if (!gameHandler.getPlayerClass(player).equals("miner")) return;
        if (!player.getInventory().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH)) return;
        Material materialToDrop;
        switch (event.getBlock().getType()) {
            case GOLD_ORE -> materialToDrop = Material.GOLD_INGOT;
            case IRON_ORE -> materialToDrop = Material.IRON_INGOT;
            default -> {
                return;
            }
        }
        for (Item item : event.getItems()) {
            if (item.getItemStack().getType().equals(materialToDrop)) {
                item.getItemStack().setType(materialToDrop);
            }
        }
    }
}

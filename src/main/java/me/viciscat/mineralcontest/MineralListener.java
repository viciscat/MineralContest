package me.viciscat.mineralcontest;

import io.papermc.paper.event.entity.EntityMoveEvent;
import me.viciscat.mineralcontest.game.GameHandler;
import me.viciscat.mineralcontest.game.RespawnPeriod;
import me.viciscat.mineralcontest.ui.ClassSelectUI;
import me.viciscat.mineralcontest.ui.TeamSelectUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
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

        if (inventory.getLocation() == null) return;
        if (!inventory.getType().equals(InventoryType.ENDER_CHEST)) {
            if (inventory.getType().equals(InventoryType.CHEST)) {
                Location arenaChestLocation = new Location(player.getWorld(), 0, gameHandler.groundHeight - 11, 0);
                if (inventory.getLocation().equals(arenaChestLocation)) {
                    if (inventory.isEmpty()) {
                        player.getWorld().getBlockAt(arenaChestLocation).setType(Material.AIR);
                    }
                }
            }
        } // Is it an EC ?
        MineralTeam chestTeam = gameHandler.getTeam(inventory.getLocation());
        if (chestTeam == null) return;
        MineralTeam playerTeam = gameHandler.playerManager.getPlayer(player).MineralTeam();
        Inventory playerInventory = player.getInventory();
        // Wrong Team
        if (playerTeam == null || chestTeam != playerTeam) {
            for (ItemStack item : inventory.getStorageContents()) {
                if (item != null) { playerInventory.addItem(item); } // Return Item
            }

            inventory.clear();
            player.sendMessage(Component.text("Wrong team chest >:( How did you get in here?"));
            return;
        }
        int finalScore = 0;
        int scoreToRemove = 0;
        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack == null) { finalScore += 0; } else {
                finalScore += gameHandler.scoreMap.getOrDefault(itemStack.getType(), 0) * itemStack.getAmount();
                inventory.remove(itemStack);
                if (itemStack.getType() == Material.REDSTONE) {
                    scoreToRemove += itemStack.getAmount();
                }
                else if (!gameHandler.scoreMap.containsKey(itemStack.getType())) {
                    playerInventory.addItem(itemStack); // If invalid item return it
                }
            }
        }
        finalScore *= playerTeam.getScoreMultiplier();
        playerTeam.addScore(finalScore);
        if (scoreToRemove <= 0) return;
        playerTeam.sendMessage(Component.text("Your team removed " + scoreToRemove + " points from other teams!"));
        for (MineralTeam team : gameHandler.getTeams()) {
            if (!team.playerInTeam(player.getUniqueId())) {
                team.setScore(team.getScore() - scoreToRemove);
                TextComponent.Builder component = Component.text();
                component.append(Component.text("-", NamedTextColor.DARK_RED));
                component.append(Component.text(scoreToRemove, NamedTextColor.DARK_RED));
                component.append(Component.text(" points!", NamedTextColor.WHITE));
                team.sendMessage(component.build());

            }
        }

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
        int rawSlot = event.getRawSlot();
        if (holder instanceof TeamSelectUI.Holder) {
            // TEAM SELECTION
            GameHandler gameHandler = map.get(player.getWorld());
            event.setCancelled(true);
            if (rawSlot >= 10 && rawSlot <= 16 && rawSlot%2==0) {
                int teamID = (rawSlot - 10) / 2;
                // If player already in the clicked team
                MineralTeam team = gameHandler.getTeam(teamID);
                assert team != null;
                MineralPlayer mineralPlayer = gameHandler.playerManager.getPlayer(player);
                if (team.playerInTeam(mineralPlayer)) {
                    player.sendMessage(Component.text("You are already in this team buckaroo!"));
                    return;
                }
                Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(MineralContest.class), () -> TeamSelectUI.openUI(player, gameHandler, inventory));
                // Leave previous team
                MineralTeam oldTeam = mineralPlayer.MineralTeam();

                if (oldTeam != null) { oldTeam.removePlayer(mineralPlayer); }
                // Send message and join new team
                player.sendMessage(Component.text("You joined the ")
                        .append(Component.text(TeamSelectUI.teamColors[teamID], TeamSelectUI.textColors[teamID]))
                        .append(Component.text(" team!")));
                team.addPlayer(mineralPlayer);

            }
        } else if (holder instanceof ClassSelectUI.Holder) {
            // CLASS SELECTION
            GameHandler gameHandler = map.get(player.getWorld());
            MineralPlayer mineralPlayer = gameHandler.playerManager.getPlayer(player);
            event.setCancelled(true);
            int classID = rawSlot - 11;
            if (classID > 4 || classID < 0) return;
            mineralPlayer.ClassString(classes[classID]);
        } else if (event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.PLAYER) {
            // MINER CLASS OR SOMETHING
            int slot = event.getSlot();
            GameHandler gameHandler = map.get(player.getWorld());
            MineralPlayer mineralPlayer = gameHandler.playerManager.getPlayer(player);
            if (!(Objects.equals(mineralPlayer.ClassString(), "miner"))) return;
            if (slot >= 9 && slot <= 17) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        if (!map.containsKey(player.getWorld())) return;
        GameHandler gameHandler = map.get(player.getWorld());
        if (event.getEntity() instanceof Player attackedPlayer) {
            boolean inSameTeam = gameHandler.playerManager.getPlayer(attackedPlayer).MineralTeam() == gameHandler.playerManager.getPlayer(player).MineralTeam();
            boolean inPreGame = gameHandler.gamePhase == GameHandler.Phase.PREGAME;
            boolean inPostGame = gameHandler.getSecondsLeft() <= 0;
            if (inSameTeam || inPreGame || inPostGame) {
                event.setCancelled(true);
                return;
            }
        }
        if (!(Objects.equals(gameHandler.playerManager.getPlayer(player).ClassString(), "warrior"))) return;
        event.setDamage(event.getDamage() * 1.25);

    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (!map.containsKey(player.getWorld())) return;
        GameHandler gameHandler = map.get(player.getWorld());
        switch (gameHandler.playerManager.getPlayer(player).ClassString()) {
            case "robust" -> event.setDamage(event.getDamage() * 0.9);
            case "agile" -> {
               if (event.getCause().equals(EntityDamageEvent.DamageCause.FALL)) event.setCancelled(true);
           }
        }

        if (gameHandler.gamePhase == GameHandler.Phase.PREGAME && event.getCause() != EntityDamageEvent.DamageCause.KILL) event.setCancelled(true);


    }



    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (!map.containsKey(player.getWorld())) {
            AttributeInstance[] attributeInstances = new AttributeInstance[]{
                    player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED),
                    player.getAttribute(Attribute.GENERIC_MAX_HEALTH)
            };
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());

            for (AttributeInstance attributeInstance : attributeInstances) {
                assert attributeInstance != null;
                for (AttributeModifier modifier : attributeInstance.getModifiers()) {
                    if (modifier.getName().contains("mineralcontest")) {
                        attributeInstance.removeModifier(modifier);
                    }
                }
            }

            return;
        }
        GameHandler gameHandler = map.get(player.getWorld());

        // Update scoreboard
        gameHandler.resetPlayerScoreboard(player);
        if (gameHandler.gamePhase == GameHandler.Phase.PREGAME) {
            Inventory playerInventory = player.getInventory();
            playerInventory.clear();
            ItemStack compass = new ItemStack(Material.COMPASS);
            ItemMeta compassMeta = compass.getItemMeta();
            compassMeta.getPersistentDataContainer().set(NamespacedKey.fromString("selection_item", MineralContest.getInstance()), PersistentDataType.BOOLEAN, true);
            compassMeta.displayName(Component.text("Right click to select your team!").decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
            compass.setItemMeta(compassMeta);
            playerInventory.setItem(8, compass);
            player.setGameMode(GameMode.ADVENTURE);

            gameHandler.pregameTeam.addPlayer(player);
        }
    }
    @EventHandler
    public void onBlockDropItem(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode().equals(GameMode.CREATIVE)) return;
        if (!map.containsKey(player.getWorld())) return;
        GameHandler gameHandler = map.get(player.getWorld());
        if (!gameHandler.playerManager.getPlayer(player).ClassString().equals("miner")) return;
        if (player.getInventory().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH)) return;
        Material materialToDrop;
        Material originalMaterial;
        switch (event.getBlockState().getType()) {
            case GOLD_ORE, DEEPSLATE_GOLD_ORE -> {
                materialToDrop = Material.GOLD_INGOT;
                originalMaterial = Material.RAW_GOLD;
            }
            case IRON_ORE, DEEPSLATE_IRON_ORE -> {
                materialToDrop = Material.IRON_INGOT;
                originalMaterial = Material.RAW_IRON;
            }
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> {
                materialToDrop = Material.COPPER_INGOT;
                originalMaterial = Material.RAW_COPPER;
            }
            default -> {
                return;
            }
        }
        for (Item item : event.getItems()) {
            if (item.getItemStack().getType().equals(originalMaterial)) {
                item.getItemStack().setType(materialToDrop);
            }
        }
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode().equals(GameMode.CREATIVE)) return;
        if (!map.containsKey(player.getWorld())) return;
        GameHandler gameHandler = map.get(player.getWorld());
        Location location = event.getBlock().getLocation();
        if (isLocationNoGood(location, gameHandler.groundHeight)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode().equals(GameMode.CREATIVE)) return;
        if (!map.containsKey(player.getWorld())) return;
        GameHandler gameHandler = map.get(player.getWorld());
        Location location = event.getBlock().getLocation();
        if (isLocationNoGood(location, gameHandler.groundHeight)) {
            event.setCancelled(true);
        }
    }

    private boolean isLocationNoGood(Location location, int groundHeight) {
        boolean inX = location.getX() <= 75 && location.getX() >= -75;
        boolean inZ = location.getZ() <= 75 && location.getZ() >= -75;
        boolean inY = location.getY() <= 255 && location.getY() >= groundHeight - 2;

        boolean inArenaX = location.getX() <= 18 && location.getX() >= -18;
        boolean inArenaZ = location.getZ() <= 18 && location.getZ() >= -18;
        boolean inArenaY = location.getY() <= groundHeight && location.getY() >= groundHeight - 15;
        return (inX && inZ && inY) || (inArenaX && inArenaY && inArenaZ);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!map.containsKey(player.getWorld())) return;
        GameHandler gameHandler = map.get(player.getWorld());
        player.setGameMode(GameMode.SPECTATOR);
        new RespawnPeriod(player, gameHandler);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!event.hasChangedPosition()) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (!map.containsKey(player.getWorld())) return;
        GameHandler gameHandler = map.get(player.getWorld());
        if (gameHandler.isInEnemyCastle(gameHandler.playerManager.getPlayer(player).MineralTeam(), event.getTo())) {
            event.setCancelled(true);
            player.sendMessage(Component.text("C'est pas ton chateau !!", TextColor.color(NamedTextColor.DARK_RED)));
        }
        // player.sendMessage(" from ", event.getFrom().toString(), " to ", event.getTo().toString());
    }

    @EventHandler
    public void onEntityMove(EntityMoveEvent event) {
        LivingEntity entity = event.getEntity();
        if (!map.containsKey(entity.getWorld())) return;
        GameHandler gameHandler = map.get(entity.getWorld());
        if (isLocationNoGood(event.getTo(), gameHandler.groundHeight) && !isLocationNoGood(event.getFrom(), gameHandler.groundHeight)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        onPlayerChangedWorld(new PlayerChangedWorldEvent(player, player.getWorld()));
        if (!map.containsKey(player.getWorld())) return;
        GameHandler gameHandler = map.get(player.getWorld());
        if (gameHandler.gamePhase == GameHandler.Phase.PREGAME || gameHandler.gamePhase == GameHandler.Phase.CLASS_SELECTING) return;
        player.setHealth(0);
    }

    @SuppressWarnings("ConstantConditions")
    @EventHandler
    public void onDropItem(PlayerDropItemEvent event){
        Player player = event.getPlayer();
        if (!map.containsKey(player.getWorld())) return;
        if (event.getItemDrop().getItemStack().getItemMeta().getPersistentDataContainer().has(NamespacedKey.fromString("selection_item", MineralContest.getInstance()))){
            event.setCancelled(true);
        }
    }
    @EventHandler
    @SuppressWarnings("ConstantConditions")
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(
                itemStack -> {
                    Boolean bool = itemStack
                            .getItemMeta().getPersistentDataContainer()
                            .get(NamespacedKey.fromString("no_drop", MineralContest.getInstance()), PersistentDataType.BOOLEAN);
                    return bool != null && bool;
                });
    }

    @EventHandler
    public void onPortalCreated(PortalCreateEvent event) {
        if (!map.containsKey(event.getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        event.getWorld().setKeepSpawnInMemory(false);
    }

    @EventHandler
    public void onPlayerLoseFood(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (!map.containsKey(player.getWorld())) return;

        int minFood = (int) MineralContest.instance.config.get("minimumFood", 10);
        if (event.getFoodLevel() < minFood) {
            event.setCancelled(true);
        }
    }
}

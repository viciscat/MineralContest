package me.viciscat.mineralcontest.game;

import me.viciscat.mineralcontest.MineralContest;
import me.viciscat.mineralcontest.MineralPlayer;
import me.viciscat.mineralcontest.MineralTeam;
import me.viciscat.mineralcontest.PlayerManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.List;

public class RespawnPeriod extends BukkitRunnable {

    Player respawningPlayer;
    GameHandler gameHandler;
    int time = 10;

    /**
     * When an object implementing interface {@code Runnable} is used
     * to create a thread, starting the thread causes the object's
     * {@code run} method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method {@code run} is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        if (!respawningPlayer.isOnline()) {
            cancel();
            return;
        }
        // DO THE RESPAWN
        if (time == -1) {
            respawningPlayer.setGameMode(GameMode.SURVIVAL);
            MineralPlayer mineralPlayer = gameHandler.playerManager.getPlayer(respawningPlayer);
            MineralTeam mineralTeam = mineralPlayer.MineralTeam();
            respawningPlayer.teleport(mineralTeam == null ? new Location(gameHandler.gameWorld, 0, gameHandler.groundHeight + 15, 0) : mineralTeam.getSpawnLocation());
            Inventory playerInventory = respawningPlayer.getInventory();
            if (mineralPlayer.ClassString().equals("miner")) {
                for (int i = 9; i < 18; i++) {
                    ItemStack barrier = new ItemStack(Material.BARRIER);
                    ItemMeta barrierMeta = barrier.getItemMeta();
                    barrierMeta.displayName(Component.text(""));
                    barrierMeta.getPersistentDataContainer().set(NamespacedKey.fromString("no_drop", MineralContest.getInstance()), PersistentDataType.BOOLEAN, true);
                    barrier.setItemMeta(barrierMeta);
                    playerInventory.setItem(i, barrier);
                }
            }
            List<ItemStack> stuff = (List<ItemStack>) MineralContest.instance.config.get("startLoot", List.of(new ItemStack(Material.SPONGE)));
            PlayerManager.equipItems(respawningPlayer, stuff);
            cancel();

            return;
        }

        // SHOW TITLE
        respawningPlayer.showTitle(Title.title(
                Component.text("Tu es mort", Style.style(TextColor.color(NamedTextColor.DARK_RED))),
                Component.text("Tu vas respawn dans ", Style.style(TextColor.color(NamedTextColor.GOLD))).append(
                        Component.text(time, TextColor.color(NamedTextColor.YELLOW))).append(
                                Component.text(" secondes !", TextColor.color(NamedTextColor.GOLD))
                ), Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO)));
        time--;
    }

    public RespawnPeriod(Player player, GameHandler gameHandler1) {
        respawningPlayer = player;
        gameHandler = gameHandler1;
        runTaskTimer(JavaPlugin.getPlugin(MineralContest.class), 20, 20);
    }
}

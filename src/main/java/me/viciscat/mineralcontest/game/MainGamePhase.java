package me.viciscat.mineralcontest.game;

import it.unimi.dsi.fastutil.Pair;
import me.viciscat.mineralcontest.MineralPlayer;
import me.viciscat.mineralcontest.MineralTeam;
import me.viciscat.mineralcontest.MineralUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Arrays;
import java.util.Collections;

public class MainGamePhase {

    static Pair<Double, Material>[] loot = new Pair[4];

    static void phase(GameHandler game) {
        loot[0] = Pair.of(1.0d, Material.EMERALD);
        loot[1] = Pair.of(2.0d, Material.DIAMOND);
        loot[2] = Pair.of(3.0d, Material.IRON_INGOT);
        loot[3] = Pair.of(3.0d, Material.GOLD_INGOT);

        int secBeforeChest = game.secondsLeft - game.nextChest;

        // CHEST SPAWNING
        if (secBeforeChest <= 10 && secBeforeChest >= 0) {
            if (secBeforeChest == 10) {
                game.gameBar = BossBar.bossBar(
                        timerComponent(secBeforeChest),
                        1,
                        BossBar.Color.BLUE,
                        BossBar.Overlay.NOTCHED_10);

                for (Player player : game.gameWorld.getPlayers()) {
                    player.showTitle(Title.title(
                            Component.text("A chest is going to appear!", Style.style(TextColor.color(NamedTextColor.BLUE))),
                            Component.text("Do /arene to teleport your team!", Style.style(TextColor.color(NamedTextColor.AQUA)))));
                    player.showBossBar(game.gameBar);
                }

            }
            game.gameBar.progress((float) secBeforeChest /10);
            game.gameBar.name(timerComponent(secBeforeChest));


            if (secBeforeChest == 0) {
                game.nextChest -= game.CHEST_PERIOD;
                for (Player player : game.gameWorld.getPlayers()) {
                    player.hideBossBar(game.gameBar);
                    // spawn the funni chest
                    game.gameWorld.getBlockAt(0, game.groundHeight - 11, 0).setType(Material.CHEST);
                    BlockState state = game.gameWorld.getBlockAt(0, game.groundHeight - 11, 0).getState();
                    if (state instanceof Chest chest) {
                        Inventory inventory = chest.getInventory();
                        chest.customName(Component.text("Arena chest"));
                        for (int i = 0; i < 27; i++) {
                            Material chosenMaterial = MineralUtils.weightedRandom(loot);
                            if (chosenMaterial == null) {
                                chosenMaterial = Material.AIR;
                            }
                            inventory.setItem(i, new ItemStack(chosenMaterial));
                        }
                        inventory.setItem(13, new ItemStack(Material.EMERALD));
                    }
                }
            }
        }

        // GAME END
        if (game.secondsLeft == 0) {
            for (Player player : game.gameWorld.getPlayers()) {
                game.pregameTeam.addPlayer(player);
                player.showTitle(Title.title(
                        Component.text("FINISH !"),
                        Component.text("")
                        ));
                player.teleport(new Location(game.gameWorld, -25, game.groundHeight + 1, 0));
                player.getInventory().clear();
                player.setGameMode(GameMode.ADVENTURE);
            }
        }
        if (game.secondsLeft == -3) {game.gameWorld.sendMessage(Component.text("LES RESULTATS !"));}

        if (game.secondsLeft == -5) {
            MineralTeam[] mineralTeams = new MineralTeam[4];
            for (int i = 0; i < 4; i++) {
                mineralTeams[i] = game.getTeam(i);
            }
            Arrays.sort(mineralTeams, Collections.reverseOrder());
            for (MineralTeam mineralTeam : mineralTeams) {
                game.gameWorld.sendMessage(mineralTeam.getTeamName().append(
                        Component.text(" -> " + mineralTeam.getScore())
                ));
            }
        }
    }

    static void updatePlayerScoreboards(GameHandler game) {

        int minutes = 0;
        int seconds = 0;
        if (game.secondsLeft > 0) {
            minutes = game.secondsLeft / 60;
            seconds = game.secondsLeft % 60;
        }
        for (MineralPlayer mineralPlayer: game.playerManager.getPlayers()) {
            int score;
            MineralTeam mineralTeam = mineralPlayer.MineralTeam();
            if (mineralTeam == null) {
                score = -1;
            } else {
                score = mineralTeam.getScore();
            }
            Scoreboard scoreboard = mineralPlayer.PlayerScoreboard();
            Objective objective = scoreboard.getObjective("mineral_contest_gui");

            if (objective != null) {
                //objective.setDisplaySlot(DisplaySlot.SIDEBAR);
                for (String entry : scoreboard.getEntries()) {
                    switch (objective.getScore(entry).getScore()) {
                        case 4 -> {
                            scoreboard.resetScores(entry);
                            String zero = seconds < 10 ? "0" : "";
                            objective.getScore("§7Time left: §b" + minutes + ":" + zero + seconds).setScore(4);
                        }
                        case 0 -> {
                            scoreboard.resetScores(entry);
                            objective.getScore("§7Points: §b" + score).setScore(0);
                        }
                        default -> {
                        }
                    }
                }
            }
        }
    }

    static Component timerComponent(int time) {
        return Component.text("Chest spawn in arena in: ",
                        Style.style(TextColor.color(NamedTextColor.AQUA)))
                .append(Component.text(time,
                        Style.style(TextColor.color(NamedTextColor.BLUE))));
    }
}

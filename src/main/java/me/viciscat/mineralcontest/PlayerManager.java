package me.viciscat.mineralcontest;

import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {

    public Map<UUID, MineralPlayer> playerMap = new HashMap<>();

    /**
     * Gets the MineralPlayer of the given player
     * if it doesn't exist, creates it
     * @param player the player
     * @return MineralPlayer of the player
     */
    public MineralPlayer getPlayer(Player player) {
        return getPlayer(player.getUniqueId());
    }

    /**
     * Gets the MineralPlayer of the given player UUID
     * if it doesn't exist, creates it
     * @param playerUUID player's UUID
     * @return MineralPlayer of the player
     */
    public MineralPlayer getPlayer(UUID playerUUID) {
        return playerMap.computeIfAbsent(playerUUID, MineralPlayer::new);
    }

    public MineralPlayer[] getPlayers() {
        return playerMap.values().toArray(new MineralPlayer[0]);
    }

    public static void equipItems(Player player, List<ItemStack> items) {
        ItemStack[] armorToEquip = new ItemStack[4];
        for (ItemStack itemStack : items) {
            if (EnchantmentTarget.ARMOR_HEAD.includes(itemStack.getType()) && armorToEquip[3] == null) {
                armorToEquip[3] = itemStack;
                continue;
            } else if (EnchantmentTarget.ARMOR_TORSO.includes(itemStack.getType()) && armorToEquip[2] == null) {
                armorToEquip[2] = itemStack;
                continue;
            } else if (EnchantmentTarget.ARMOR_LEGS.includes(itemStack.getType()) && armorToEquip[1] == null) {
                armorToEquip[1] = itemStack;
                continue;
            } else if (EnchantmentTarget.ARMOR_FEET.includes(itemStack.getType()) && armorToEquip[0] == null) {
                armorToEquip[0] = itemStack;
                continue;
            }
            player.getInventory().addItem(itemStack);
        }
        player.getEquipment().setArmorContents(armorToEquip);
    }


}

package me.wobble.wbountyhunters.gui;

import me.wobble.wbountyhunters.WBountyHunters;
import me.wobble.wbountyhunters.model.Bounty;
import me.wobble.wbountyhunters.util.ChatUtil;
import me.wobble.wbountyhunters.util.TextStyleUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public final class BountyConfirmGUI {

    public static final int BACK_SLOT = 45;
    public static final int CLOSE_SLOT = 49;
    public static final int PLACE_SLOT = 29;
    public static final int INFO_SLOT = 31;
    public static final int SET_SLOT = 33;
    public static final int REMOVE_SLOT = 35;

    private final WBountyHunters plugin;

    public BountyConfirmGUI(WBountyHunters plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Bounty bounty) {
        Inventory inventory = ManagedGui.createInventory(ManagedGui.Type.BOUNTY_CONFIRM, 54, ChatUtil.mm("<dark_gray>ʙᴏᴜɴᴛʏ ᴄᴏɴꜰɪʀᴍ"));
        fillBackground(inventory);

        OfflinePlayer target = Bukkit.getOfflinePlayer(bounty.getTargetId());
        String storedName = bounty.getTargetName();
        String name = storedName != null && !storedName.isBlank()
                ? storedName
                : (target.getName() == null ? "Unknown" : target.getName());

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.setOwningPlayer(target);
        skullMeta.displayName(ChatUtil.mm("<gold>" + name + "</gold>"));
        skullMeta.lore(List.of(
                ChatUtil.mm("<gray>Target:</gray> <yellow>" + name + "</yellow>"),
                ChatUtil.mm("<gray>Current bounty:</gray> <gold>" + plugin.getBountyService().format(bounty.getAmount()) + "</gold>"),
                ChatUtil.mm("<dark_gray>-"),
                ChatUtil.mm(TextStyleUtil.hint("choose an action below"))
        ));
        skullMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        head.setItemMeta(skullMeta);
        inventory.setItem(13, head);

        inventory.setItem(BACK_SLOT, simpleItem(
                Material.ARROW,
                "<yellow>" + TextStyleUtil.smallCaps("Back"),
                List.of(ChatUtil.mm(TextStyleUtil.hint("return to bounty list")))
        ));

        inventory.setItem(CLOSE_SLOT, simpleItem(
                Material.BARRIER,
                "<red>" + TextStyleUtil.smallCaps("Close"),
                List.of(ChatUtil.mm(TextStyleUtil.hint("close the menu")))
        ));

        inventory.setItem(PLACE_SLOT, simpleItem(
                Material.EMERALD,
                "<green>" + TextStyleUtil.smallCaps("Place Bounty"),
                List.of(
                        ChatUtil.mm("<gray>Target:</gray> <yellow>" + name + "</yellow>"),
                        ChatUtil.mm("<dark_gray>-"),
                        ChatUtil.mm(TextStyleUtil.hint("click to enter amount in chat"))
                )
        ));

        inventory.setItem(INFO_SLOT, simpleItem(
                Material.BOOK,
                "<gold>" + TextStyleUtil.smallCaps("Target Info"),
                List.of(
                        ChatUtil.mm("<gray>Target:</gray> <yellow>" + name + "</yellow>"),
                        ChatUtil.mm("<gray>Bounty:</gray> <gold>" + plugin.getBountyService().format(bounty.getAmount()) + "</gold>"),
                        ChatUtil.mm("<dark_gray>-"),
                        ChatUtil.mm(TextStyleUtil.hint("click to view info in chat"))
                )
        ));

        if (player.hasPermission("wobble.bounty.admin")) {
            inventory.setItem(SET_SLOT, simpleItem(
                    Material.ANVIL,
                    "<gold>" + TextStyleUtil.smallCaps("Set Bounty"),
                    List.of(
                            ChatUtil.mm("<gray>Target:</gray> <yellow>" + name + "</yellow>"),
                            ChatUtil.mm("<dark_gray>-"),
                            ChatUtil.mm(TextStyleUtil.hint("click to enter new amount in chat"))
                    )
            ));

            inventory.setItem(REMOVE_SLOT, simpleItem(
                    Material.TNT,
                    "<red>" + TextStyleUtil.smallCaps("Remove Bounty"),
                    List.of(
                            ChatUtil.mm("<gray>Target:</gray> <yellow>" + name + "</yellow>"),
                            ChatUtil.mm("<dark_gray>-"),
                            ChatUtil.mm(TextStyleUtil.hint("click to remove this bounty"))
                    )
            ));
        }

        player.openInventory(inventory);
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = simpleItem(Material.BLACK_STAINED_GLASS_PANE, "<dark_gray> ", List.of());
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    private ItemStack simpleItem(Material material, String name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ChatUtil.mm(name));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }
}

package me.wobble.wobblebounty.listener;

import me.wobble.wobblebounty.WobbleBounty;
import me.wobble.wobblebounty.gui.BountyGUI;
import me.wobble.wobblebounty.model.Bounty;
import me.wobble.wobblebounty.service.BountyService;
import me.wobble.wobblebounty.util.ChatUtil;
import me.wobble.wobblebounty.util.SoundUtil;
import me.wobble.wobblebounty.util.TextStyleUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BountyMenuListener implements Listener {

    private final WobbleBounty plugin;
    private final BountyGUI bountyGUI;
    private final Map<UUID, Integer> pages = new HashMap<>();
    private final Map<UUID, BountyService.SortType> sorts = new HashMap<>();
    private final Map<UUID, String> searches = new HashMap<>();
    private final Map<UUID, Boolean> awaitingSearch = new HashMap<>();

    public BountyMenuListener(WobbleBounty plugin) {
        this.plugin = plugin;
        this.bountyGUI = new BountyGUI(plugin, plugin.getBountyService());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().title() == null) {
            return;
        }

        String rawTitle = plugin.getConfig().getString("gui.title", "<dark_gray>ʙᴏᴜɴᴛʏ");
        if (!event.getView().title().equals(ChatUtil.mm(rawTitle))) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClickedInventory() == null) {
            return;
        }

        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        UUID playerId = player.getUniqueId();
        int slot = event.getRawSlot();
        int currentPage = pages.getOrDefault(playerId, 1);
        BountyService.SortType sortType = sorts.getOrDefault(playerId, BountyService.SortType.HIGHEST);
        String searchQuery = searches.get(playerId);

        int closeSlot = plugin.getConfig().getInt("gui.slots.close", 49);
        int prevSlot = plugin.getConfig().getInt("gui.slots.previous-page", 45);
        int nextSlot = plugin.getConfig().getInt("gui.slots.next-page", 53);
        int infoSlot = plugin.getConfig().getInt("gui.slots.info", 4);
        int sortSlot = BountyGUI.SORT_SLOT;
        int searchSlot = BountyGUI.SEARCH_SLOT;
        int resetSlot = BountyGUI.RESET_SLOT;

        if (slot == closeSlot) {
            SoundUtil.playClick(plugin, player);
            player.closeInventory();
            return;
        }

        if (slot == prevSlot) {
            if (currentPage <= 1) {
                SoundUtil.playError(plugin, player);
                return;
            }

            int targetPage = currentPage - 1;
            pages.put(playerId, targetPage);
            SoundUtil.playClick(plugin, player);
            bountyGUI.open(player, targetPage, sortType, searchQuery);
            return;
        }

        if (slot == nextSlot) {
            int maxPage = bountyGUI.getMaxPage(sortType, searchQuery);
            if (currentPage >= maxPage) {
                SoundUtil.playError(plugin, player);
                return;
            }

            int targetPage = currentPage + 1;
            pages.put(playerId, targetPage);
            SoundUtil.playClick(plugin, player);
            bountyGUI.open(player, targetPage, sortType, searchQuery);
            return;
        }

        if (slot == infoSlot) {
            SoundUtil.playClick(plugin, player);
            int results = bountyGUI.getResultCount(sortType, searchQuery);

            player.sendMessage(ChatUtil.mm("<dark_gray>— <gold>" + TextStyleUtil.smallCaps("bounty info") + " <dark_gray>—"));
            player.sendMessage(ChatUtil.mm("<gray>Page:</gray> <yellow>" + currentPage + "</yellow><gray>/</gray><yellow>" + bountyGUI.getMaxPage(sortType, searchQuery) + "</yellow>"));
            player.sendMessage(ChatUtil.mm("<gray>Sort:</gray> <yellow>" + TextStyleUtil.sortDisplay(sortType.name()) + "</yellow>"));
            player.sendMessage(ChatUtil.mm("<gray>Search:</gray> <yellow>" + TextStyleUtil.displaySearch(searchQuery) + "</yellow>"));
            player.sendMessage(ChatUtil.mm("<gray>Results:</gray> <gold>" + results + "</gold>"));
            return;
        }

        if (slot == sortSlot) {
            BountyService.SortType nextSort = sortType == BountyService.SortType.HIGHEST
                    ? BountyService.SortType.LOWEST
                    : BountyService.SortType.HIGHEST;

            sorts.put(playerId, nextSort);
            pages.put(playerId, 1);

            SoundUtil.playClick(plugin, player);
            player.sendMessage(ChatUtil.mm("<gray>Sort switched to:</gray> <yellow>" + TextStyleUtil.sortDisplay(nextSort.name()) + "</yellow>"));
            bountyGUI.open(player, 1, nextSort, searchQuery);
            return;
        }

        if (slot == searchSlot) {
            if (event.isShiftClick()) {
                searches.remove(playerId);
                awaitingSearch.remove(playerId);
                pages.put(playerId, 1);

                SoundUtil.playClick(plugin, player);
                player.sendMessage(ChatUtil.mm("<gray>Search cleared.</gray>"));
                bountyGUI.open(player, 1, sortType, null);
                return;
            }

            awaitingSearch.put(playerId, true);
            SoundUtil.playClick(plugin, player);
            player.closeInventory();
            player.sendMessage(ChatUtil.mm("<dark_gray>— <gold>" + TextStyleUtil.smallCaps("search mode") + " <dark_gray>—"));
            player.sendMessage(ChatUtil.mm("<gray>Type a player name in chat.</gray>"));
            player.sendMessage(ChatUtil.mm("<gray>Type:</gray> <yellow>cancel</yellow> <gray>to stop searching.</gray>"));
            return;
        }

        if (slot == resetSlot) {
            if (searchQuery == null || searchQuery.isBlank()) {
                SoundUtil.playError(plugin, player);
                return;
            }

            searches.remove(playerId);
            pages.put(playerId, 1);
            SoundUtil.playClick(plugin, player);
            player.sendMessage(ChatUtil.mm("<gray>Search filter reset.</gray>"));
            bountyGUI.open(player, 1, sortType, null);
            return;
        }

        Bounty clicked = bountyGUI.getBountyBySlot(slot, currentPage, sortType, searchQuery);
        if (clicked == null) {
            SoundUtil.playError(plugin, player);
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(clicked.getTargetId());
        String name = target.getName() == null ? clicked.getTargetId().toString() : target.getName();

        SoundUtil.playSuccess(plugin, player);
        player.sendMessage(ChatUtil.mm("<dark_gray>— <gold>" + TextStyleUtil.smallCaps("bounty target") + " <dark_gray>—"));
        player.sendMessage(ChatUtil.mm("<gray>Target:</gray> <yellow>" + name + "</yellow>"));
        player.sendMessage(ChatUtil.mm("<gray>Bounty:</gray> <gold>" + plugin.getBountyService().format(clicked.getAmount()) + "</gold>"));
        player.sendMessage(ChatUtil.mm("<gray>Command:</gray> <yellow>/bounty check " + name + "</yellow>"));
    }

    public boolean isAwaitingSearch(UUID playerId) {
        return awaitingSearch.getOrDefault(playerId, false);
    }

    public void stopAwaitingSearch(UUID playerId) {
        awaitingSearch.remove(playerId);
    }

    public void applySearch(Player player, String query) {
        UUID playerId = player.getUniqueId();
        stopAwaitingSearch(playerId);

        BountyService.SortType sortType = sorts.getOrDefault(playerId, BountyService.SortType.HIGHEST);

        if (query == null || query.isBlank()) {
            searches.remove(playerId);
            pages.put(playerId, 1);
            bountyGUI.open(player, 1, sortType, null);
            return;
        }

        searches.put(playerId, query);
        pages.put(playerId, 1);

        int results = bountyGUI.getResultCount(sortType, query);
        player.sendMessage(ChatUtil.mm("<gray>Search set to:</gray> <yellow>" + query + "</yellow>"));
        player.sendMessage(ChatUtil.mm("<gray>Results found:</gray> <gold>" + results + "</gold>"));

        bountyGUI.open(player, 1, sortType, query);
    }
}

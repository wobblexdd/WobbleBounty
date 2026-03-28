package me.wobble.wobblebounty.listener;

import me.wobble.wobblebounty.WobbleBounty;
import me.wobble.wobblebounty.gui.BountyConfirmGUI;
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
import java.util.Optional;
import java.util.UUID;

public final class BountyMenuListener implements Listener {

    private record GuiContext(int page, BountyService.SortType sortType, String searchQuery) {}

    private final WobbleBounty plugin;
    private final BountyGUI bountyGUI;
    private final BountyConfirmGUI confirmGUI;

    private final Map<UUID, Integer> pages = new HashMap<>();
    private final Map<UUID, BountyService.SortType> sorts = new HashMap<>();
    private final Map<UUID, String> searches = new HashMap<>();

    private final Map<UUID, Boolean> awaitingSearch = new HashMap<>();
    private final Map<UUID, UUID> awaitingPlaceTarget = new HashMap<>();
    private final Map<UUID, UUID> awaitingSetTarget = new HashMap<>();

    private final Map<UUID, UUID> confirmTargets = new HashMap<>();
    private final Map<UUID, GuiContext> confirmContexts = new HashMap<>();

    public BountyMenuListener(WobbleBounty plugin) {
        this.plugin = plugin;
        this.bountyGUI = new BountyGUI(plugin, plugin.getBountyService());
        this.confirmGUI = new BountyConfirmGUI(plugin);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().title() == null) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClickedInventory() == null) {
            return;
        }

        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        if (event.getView().title().equals(ChatUtil.mm(plugin.getConfig().getString("gui.title", "<dark_gray>ʙᴏᴜɴᴛʏ")))) {
            handleMainMenu(event, player);
            return;
        }

        if (event.getView().title().equals(ChatUtil.mm("<dark_gray>ʙᴏᴜɴᴛʏ ᴄᴏɴꜰɪʀᴍ"))) {
            handleConfirmMenu(event, player);
        }
    }

    private void handleMainMenu(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        UUID playerId = player.getUniqueId();
        int slot = event.getRawSlot();
        int currentPage = pages.getOrDefault(playerId, 1);
        BountyService.SortType sortType = sorts.getOrDefault(playerId, BountyService.SortType.HIGHEST);
        String searchQuery = searches.get(playerId);

        int closeSlot = plugin.getConfig().getInt("gui.slots.close", 49);
        int prevSlot = plugin.getConfig().getInt("gui.slots.previous-page", 45);
        int nextSlot = plugin.getConfig().getInt("gui.slots.next-page", 53);
        int infoSlot = plugin.getConfig().getInt("gui.slots.info", 4);

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

        if (slot == BountyGUI.SORT_SLOT) {
            BountyService.SortType nextSort = nextSort(sortType);
            sorts.put(playerId, nextSort);
            pages.put(playerId, 1);

            SoundUtil.playClick(plugin, player);
            player.sendMessage(ChatUtil.mm("<gray>Sort switched to:</gray> <yellow>" + TextStyleUtil.sortDisplay(nextSort.name()) + "</yellow>"));
            bountyGUI.open(player, 1, nextSort, searchQuery);
            return;
        }

        if (slot == BountyGUI.SEARCH_SLOT) {
            awaitingSearch.put(playerId, true);
            awaitingPlaceTarget.remove(playerId);
            awaitingSetTarget.remove(playerId);

            SoundUtil.playClick(plugin, player);
            player.closeInventory();
            player.sendMessage(ChatUtil.mm("<dark_gray>— <gold>" + TextStyleUtil.smallCaps("search mode") + " <dark_gray>—"));
            player.sendMessage(ChatUtil.mm("<gray>Type a player name in chat.</gray>"));
            player.sendMessage(ChatUtil.mm("<gray>Type:</gray> <yellow>cancel</yellow> <gray>to stop searching.</gray>"));
            return;
        }

        if (slot == BountyGUI.RESET_SLOT) {
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

        confirmTargets.put(playerId, clicked.getTargetId());
        confirmContexts.put(playerId, new GuiContext(currentPage, sortType, searchQuery));

        SoundUtil.playClick(plugin, player);
        confirmGUI.open(player, clicked);
    }

    private void handleConfirmMenu(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        UUID playerId = player.getUniqueId();
        UUID targetId = confirmTargets.get(playerId);
        GuiContext context = confirmContexts.get(playerId);

        if (targetId == null || context == null) {
            SoundUtil.playError(plugin, player);
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();

        if (slot == BountyConfirmGUI.CLOSE_SLOT) {
            SoundUtil.playClick(plugin, player);
            player.closeInventory();
            return;
        }

        if (slot == BountyConfirmGUI.BACK_SLOT) {
            SoundUtil.playClick(plugin, player);
            bountyGUI.open(player, context.page(), context.sortType(), context.searchQuery());
            return;
        }

        if (slot == BountyConfirmGUI.INFO_SLOT) {
            Optional<Bounty> optional = plugin.getBountyService().getBounty(targetId);
            if (optional.isEmpty()) {
                SoundUtil.playError(plugin, player);
                player.sendMessage(ChatUtil.mm("<red>This bounty no longer exists.</red>"));
                bountyGUI.open(player, context.page(), context.sortType(), context.searchQuery());
                return;
            }

            Bounty bounty = optional.get();
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
            String name = target.getName() == null ? "Unknown" : target.getName();

            SoundUtil.playSuccess(plugin, player);
            player.sendMessage(ChatUtil.mm("<dark_gray>— <gold>" + TextStyleUtil.smallCaps("bounty target") + " <dark_gray>—"));
            player.sendMessage(ChatUtil.mm("<gray>Target:</gray> <yellow>" + name + "</yellow>"));
            player.sendMessage(ChatUtil.mm("<gray>Bounty:</gray> <gold>" + plugin.getBountyService().format(bounty.getAmount()) + "</gold>"));
            player.sendMessage(ChatUtil.mm("<gray>Created:</gray> <yellow>" + relativeTime(bounty.getCreatedAt()) + "</yellow>"));
            return;
        }

        if (slot == BountyConfirmGUI.PLACE_SLOT) {
            awaitingSearch.remove(playerId);
            awaitingSetTarget.remove(playerId);
            awaitingPlaceTarget.put(playerId, targetId);

            SoundUtil.playClick(plugin, player);
            player.closeInventory();

            OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
            String name = target.getName() == null ? "Unknown" : target.getName();

            player.sendMessage(ChatUtil.mm("<dark_gray>— <gold>" + TextStyleUtil.smallCaps("place bounty") + " <dark_gray>—"));
            player.sendMessage(ChatUtil.mm("<gray>Target:</gray> <yellow>" + name + "</yellow>"));
            player.sendMessage(ChatUtil.mm("<gray>Type amount in chat.</gray>"));
            player.sendMessage(ChatUtil.mm("<gray>Type:</gray> <yellow>cancel</yellow> <gray>to stop.</gray>"));
            return;
        }

        if (slot == BountyConfirmGUI.SET_SLOT && player.hasPermission("wobble.bounty.admin")) {
            awaitingSearch.remove(playerId);
            awaitingPlaceTarget.remove(playerId);
            awaitingSetTarget.put(playerId, targetId);

            SoundUtil.playClick(plugin, player);
            player.closeInventory();

            OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
            String name = target.getName() == null ? "Unknown" : target.getName();

            player.sendMessage(ChatUtil.mm("<dark_gray>— <gold>" + TextStyleUtil.smallCaps("set bounty") + " <dark_gray>—"));
            player.sendMessage(ChatUtil.mm("<gray>Target:</gray> <yellow>" + name + "</yellow>"));
            player.sendMessage(ChatUtil.mm("<gray>Type new amount in chat.</gray>"));
            player.sendMessage(ChatUtil.mm("<gray>Use:</gray> <yellow>0</yellow> <gray>to remove it.</gray>"));
            player.sendMessage(ChatUtil.mm("<gray>Type:</gray> <yellow>cancel</yellow> <gray>to stop.</gray>"));
            return;
        }

        if (slot == BountyConfirmGUI.REMOVE_SLOT && player.hasPermission("wobble.bounty.admin")) {
            BountyService.RemoveResult result = plugin.getBountyService().removeBounty(targetId);
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
            String name = target.getName() == null ? "Unknown" : target.getName();

            switch (result) {
                case SUCCESS -> {
                    SoundUtil.playSuccess(plugin, player);
                    player.sendMessage(ChatUtil.mm("<green>Removed bounty from <yellow>" + name + "</yellow>.</green>"));
                }
                case NOT_FOUND -> {
                    SoundUtil.playError(plugin, player);
                    player.sendMessage(ChatUtil.mm("<red>No bounty found for <yellow>" + name + "</yellow>.</red>"));
                }
            }

            bountyGUI.open(player, 1, context.sortType(), context.searchQuery());
        }
    }

    public boolean hasPendingInput(UUID playerId) {
        return awaitingSearch.getOrDefault(playerId, false)
                || awaitingPlaceTarget.containsKey(playerId)
                || awaitingSetTarget.containsKey(playerId);
    }

    public void cancelAllInputs(UUID playerId) {
        awaitingSearch.remove(playerId);
        awaitingPlaceTarget.remove(playerId);
        awaitingSetTarget.remove(playerId);
    }

    public void handleChatInput(Player player, String input) {
        UUID playerId = player.getUniqueId();

        if (input.equalsIgnoreCase("cancel")) {
            cancelAllInputs(playerId);
            SoundUtil.playClick(plugin, player);
            player.sendMessage(ChatUtil.mm("<gray>Input cancelled.</gray>"));
            return;
        }

        if (awaitingSearch.getOrDefault(playerId, false)) {
            awaitingSearch.remove(playerId);
            applySearch(player, input);
            return;
        }

        if (awaitingPlaceTarget.containsKey(playerId)) {
            UUID targetId = awaitingPlaceTarget.remove(playerId);
            handlePlaceAmountInput(player, targetId, input);
            return;
        }

        if (awaitingSetTarget.containsKey(playerId)) {
            UUID targetId = awaitingSetTarget.remove(playerId);
            handleSetAmountInput(player, targetId, input);
        }
    }

    private void handlePlaceAmountInput(Player player, UUID targetId, String input) {
        double amount;
        try {
            amount = Double.parseDouble(input);
        } catch (NumberFormatException exception) {
            SoundUtil.playError(plugin, player);
            player.sendMessage(ChatUtil.message(plugin, "invalid-amount"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
        BountyService.PlaceResult result = plugin.getBountyService().placeBounty(player, target, amount);

        switch (result) {
            case SUCCESS -> {
                player.sendMessage(ChatUtil.message(
                        plugin,
                        "bounty-placed",
                        "{amount}", plugin.getBountyService().format(amount),
                        "{target}", target.getName() == null ? "Unknown" : target.getName()
                ));
                SoundUtil.playSuccess(plugin, player);

                if (plugin.getConfig().getBoolean("bounty.broadcast-on-place", true)) {
                    Bukkit.broadcast(ChatUtil.message(
                            plugin,
                            "broadcast-place",
                            "{player}", player.getName(),
                            "{amount}", plugin.getBountyService().format(amount),
                            "{target}", target.getName() == null ? "Unknown" : target.getName()
                    ));
                }
            }
            case INVALID_AMOUNT -> {
                player.sendMessage(ChatUtil.message(plugin, "invalid-amount"));
                SoundUtil.playError(plugin, player);
            }
            case TOO_LOW -> {
                player.sendMessage(ChatUtil.message(
                        plugin,
                        "amount-too-low",
                        "{min}", plugin.getBountyService().format(plugin.getBountyService().getMinAmount())
                ));
                SoundUtil.playError(plugin, player);
            }
            case NOT_ENOUGH_MONEY -> {
                player.sendMessage(ChatUtil.message(plugin, "not-enough-money"));
                SoundUtil.playError(plugin, player);
            }
            case SELF_TARGET -> {
                player.sendMessage(ChatUtil.message(plugin, "cannot-target-self"));
                SoundUtil.playError(plugin, player);
            }
        }
    }

    private void handleSetAmountInput(Player player, UUID targetId, String input) {
        if (!player.hasPermission("wobble.bounty.admin")) {
            SoundUtil.playError(plugin, player);
            player.sendMessage(ChatUtil.message(plugin, "no-permission"));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(input);
        } catch (NumberFormatException exception) {
            SoundUtil.playError(plugin, player);
            player.sendMessage(ChatUtil.message(plugin, "invalid-amount"));
            return;
        }

        BountyService.SetResult result = plugin.getBountyService().setBounty(targetId, amount);
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
        String name = target.getName() == null ? "Unknown" : target.getName();

        switch (result) {
            case SUCCESS -> {
                SoundUtil.playSuccess(plugin, player);
                player.sendMessage(ChatUtil.mm("<green>Set bounty for <yellow>" + name + "</yellow> to <gold>" + plugin.getBountyService().format(amount) + "</gold>.</green>"));
            }
            case INVALID_AMOUNT -> {
                SoundUtil.playError(plugin, player);
                player.sendMessage(ChatUtil.message(plugin, "invalid-amount"));
            }
        }
    }

    public void applySearch(Player player, String query) {
        UUID playerId = player.getUniqueId();
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
        SoundUtil.playSuccess(plugin, player);
        player.sendMessage(ChatUtil.mm("<gray>Search set to:</gray> <yellow>" + query + "</yellow>"));
        player.sendMessage(ChatUtil.mm("<gray>Results found:</gray> <gold>" + results + "</gold>"));

        bountyGUI.open(player, 1, sortType, query);
    }

    private BountyService.SortType nextSort(BountyService.SortType current) {
        return switch (current) {
            case HIGHEST -> BountyService.SortType.LOWEST;
            case LOWEST -> BountyService.SortType.NEWEST;
            case NEWEST -> BountyService.SortType.OLDEST;
            case OLDEST -> BountyService.SortType.ALPHABETICAL;
            case ALPHABETICAL -> BountyService.SortType.HIGHEST;
        };
    }

    private String relativeTime(long millis) {
        if (millis <= 0L) {
            return "Unknown";
        }

        long diffSeconds = Math.max(0L, (System.currentTimeMillis() - millis) / 1000L);
        long minutes = diffSeconds / 60L;
        long hours = minutes / 60L;
        long days = hours / 24L;

        if (days > 0) {
            return days + "d ago";
        }
        if (hours > 0) {
            return hours + "h ago";
        }
        if (minutes > 0) {
            return minutes + "m ago";
        }
        return diffSeconds + "s ago";
    }
}

package me.wobble.wbountyhunters.listener;

import me.wobble.wbountyhunters.WBountyHunters;
import me.wobble.wbountyhunters.gui.BountyConfirmGUI;
import me.wobble.wbountyhunters.gui.BountyGUI;
import me.wobble.wbountyhunters.gui.ManagedGui;
import me.wobble.wbountyhunters.model.Bounty;
import me.wobble.wbountyhunters.service.BountyService;
import me.wobble.wbountyhunters.util.ChatUtil;
import me.wobble.wbountyhunters.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class BountyMenuListener implements Listener {

    private final WBountyHunters plugin;
    private final BountyGUI bountyGUI;
    private final BountyConfirmGUI confirmGUI;

    private final Map<UUID, Integer> pages = new HashMap<>();
    private final Map<UUID, BountyService.SortType> sortTypes = new HashMap<>();
    private final Map<UUID, String> searchQueries = new HashMap<>();
    private final Map<UUID, Boolean> awaitingSearch = new HashMap<>();
    private final Map<UUID, PendingAction> pendingActions = new HashMap<>();
    private final Map<UUID, Bounty> selectedBounties = new HashMap<>();

    private record PendingAction(ActionType type, UUID targetId, String targetName) {}

    public enum ActionType {
        PLACE,
        SET
    }

    public BountyMenuListener(WBountyHunters plugin) {
        this.plugin = plugin;
        this.bountyGUI = new BountyGUI(plugin, plugin.getBountyService());
        this.confirmGUI = new BountyConfirmGUI(plugin);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null) {
            return;
        }
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        ManagedGui.Type guiType = ManagedGui.getType(event.getView());
        if (guiType == null) {
            return;
        }

        switch (guiType) {
            case BOUNTY_MAIN -> handleMainMenu(event, player);
            case BOUNTY_CONFIRM -> handleConfirmMenu(event, player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearState(event.getPlayer().getUniqueId());
    }

    private void handleMainMenu(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        UUID playerId = player.getUniqueId();
        int slot = event.getRawSlot();
        int page = pages.getOrDefault(playerId, 1);
        BountyService.SortType sortType = sortTypes.getOrDefault(playerId, BountyService.SortType.HIGHEST);
        String searchQuery = searchQueries.get(playerId);

        int prevSlot = plugin.getConfig().getInt("gui.slots.previous-page", 45);
        int closeSlot = plugin.getConfig().getInt("gui.slots.close", 49);
        int nextSlot = plugin.getConfig().getInt("gui.slots.next-page", 53);

        if (slot == closeSlot) {
            SoundUtil.playClick(plugin, player);
            player.closeInventory();
            return;
        }

        if (slot == prevSlot) {
            if (page <= 1) {
                SoundUtil.playError(plugin, player);
                return;
            }
            pages.put(playerId, page - 1);
            SoundUtil.playClick(plugin, player);
            bountyGUI.open(player, page - 1, sortType, searchQuery);
            return;
        }

        if (slot == nextSlot) {
            int maxPage = bountyGUI.getMaxPage(sortType, searchQuery);
            if (page >= maxPage) {
                SoundUtil.playError(plugin, player);
                return;
            }
            pages.put(playerId, page + 1);
            SoundUtil.playClick(plugin, player);
            bountyGUI.open(player, page + 1, sortType, searchQuery);
            return;
        }

        if (slot == BountyGUI.SORT_SLOT) {
            BountyService.SortType next = nextSort(sortType);
            sortTypes.put(playerId, next);
            pages.put(playerId, 1);
            SoundUtil.playClick(plugin, player);
            bountyGUI.open(player, 1, next, searchQuery);
            return;
        }

        if (slot == BountyGUI.SEARCH_SLOT) {
            awaitingSearch.put(playerId, true);
            player.closeInventory();
            SoundUtil.playClick(plugin, player);
            player.sendMessage(ChatUtil.mm("<gray>Type player name in chat.</gray>"));
            player.sendMessage(ChatUtil.mm("<gray>Type:</gray> <yellow>cancel</yellow> <gray>to abort.</gray>"));
            return;
        }

        if (slot == BountyGUI.RESET_SLOT) {
            searchQueries.remove(playerId);
            pages.put(playerId, 1);
            SoundUtil.playClick(plugin, player);
            bountyGUI.open(player, 1, sortType, null);
            return;
        }

        Bounty clicked = bountyGUI.getBountyBySlot(slot, page, sortType, searchQuery);
        if (clicked == null) {
            SoundUtil.playError(plugin, player);
            return;
        }

        selectedBounties.put(playerId, clicked);
        SoundUtil.playClick(plugin, player);
        confirmGUI.open(player, clicked);
    }

    private void handleConfirmMenu(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        UUID playerId = player.getUniqueId();
        int slot = event.getRawSlot();
        Bounty selected = selectedBounties.get(playerId);
        if (selected == null) {
            SoundUtil.playError(plugin, player);
            player.closeInventory();
            return;
        }

        if (slot == BountyConfirmGUI.CLOSE_SLOT) {
            SoundUtil.playClick(plugin, player);
            player.closeInventory();
            return;
        }

        if (slot == BountyConfirmGUI.BACK_SLOT) {
            int page = pages.getOrDefault(playerId, 1);
            BountyService.SortType sortType = sortTypes.getOrDefault(playerId, BountyService.SortType.HIGHEST);
            String searchQuery = searchQueries.get(playerId);
            SoundUtil.playClick(plugin, player);
            bountyGUI.open(player, page, sortType, searchQuery);
            return;
        }

        String targetName = selected.getTargetName() == null || selected.getTargetName().isBlank() ? "Unknown" : selected.getTargetName();

        if (slot == BountyConfirmGUI.INFO_SLOT) {
            Optional<Bounty> bounty = plugin.getBountyService().getBounty(selected.getTargetId());
            if (bounty.isEmpty()) {
                player.sendMessage(ChatUtil.mm("<red>No bounty found for <yellow>" + targetName + "</yellow>.</red>"));
                SoundUtil.playError(plugin, player);
                return;
            }

            Bounty value = bounty.get();
            player.sendMessage(ChatUtil.mm("<dark_gray>- <gold>Bounty Info</gold> <dark_gray>-"));
            player.sendMessage(ChatUtil.mm("<gray>Target:</gray> <yellow>" + targetName + "</yellow>"));
            player.sendMessage(ChatUtil.mm("<gray>Amount:</gray> <gold>" + plugin.getBountyService().format(value.getAmount()) + "</gold>"));
            player.sendMessage(ChatUtil.mm("<gray>Created:</gray> <yellow>" + value.getCreatedAt() + "</yellow>"));
            SoundUtil.playSuccess(plugin, player);
            return;
        }

        if (slot == BountyConfirmGUI.PLACE_SLOT) {
            pendingActions.put(playerId, new PendingAction(ActionType.PLACE, selected.getTargetId(), targetName));
            player.closeInventory();
            player.sendMessage(ChatUtil.mm("<gray>Enter bounty amount for <yellow>" + targetName + "</yellow> in chat.</gray>"));
            player.sendMessage(ChatUtil.mm("<gray>Type:</gray> <yellow>cancel</yellow> <gray>to abort.</gray>"));
            SoundUtil.playClick(plugin, player);
            return;
        }

        if (slot == BountyConfirmGUI.SET_SLOT) {
            if (!player.hasPermission("wobble.bounty.admin")) {
                player.sendMessage(ChatUtil.message(plugin, "no-permission"));
                SoundUtil.playError(plugin, player);
                return;
            }

            pendingActions.put(playerId, new PendingAction(ActionType.SET, selected.getTargetId(), targetName));
            player.closeInventory();
            player.sendMessage(ChatUtil.mm("<gray>Enter new bounty amount for <yellow>" + targetName + "</yellow> in chat.</gray>"));
            player.sendMessage(ChatUtil.mm("<gray>Type:</gray> <yellow>cancel</yellow> <gray>to abort.</gray>"));
            SoundUtil.playClick(plugin, player);
            return;
        }

        if (slot == BountyConfirmGUI.REMOVE_SLOT) {
            if (!player.hasPermission("wobble.bounty.admin")) {
                player.sendMessage(ChatUtil.message(plugin, "no-permission"));
                SoundUtil.playError(plugin, player);
                return;
            }

            BountyService.RemoveResult result = plugin.getBountyService().removeBounty(selected.getTargetId());
            switch (result) {
                case SUCCESS -> {
                    player.sendMessage(ChatUtil.mm("<green>Removed bounty from <yellow>" + targetName + "</yellow>.</green>"));
                    SoundUtil.playSuccess(plugin, player);
                }
                case NOT_FOUND -> {
                    player.sendMessage(ChatUtil.mm("<red>No bounty found for <yellow>" + targetName + "</yellow>.</red>"));
                    SoundUtil.playError(plugin, player);
                }
            }

            int page = pages.getOrDefault(playerId, 1);
            BountyService.SortType sortType = sortTypes.getOrDefault(playerId, BountyService.SortType.HIGHEST);
            String searchQuery = searchQueries.get(playerId);
            bountyGUI.open(player, page, sortType, searchQuery);
            return;
        }

        SoundUtil.playError(plugin, player);
    }

    public boolean isAwaitingSearch(UUID playerId) {
        return awaitingSearch.getOrDefault(playerId, false);
    }

    public void cancelAwaitingSearch(UUID playerId) {
        awaitingSearch.remove(playerId);
    }

    public void clearState(UUID playerId) {
        pages.remove(playerId);
        sortTypes.remove(playerId);
        searchQueries.remove(playerId);
        awaitingSearch.remove(playerId);
        pendingActions.remove(playerId);
        selectedBounties.remove(playerId);
    }

    public void applySearch(Player player, String query) {
        UUID playerId = player.getUniqueId();
        awaitingSearch.remove(playerId);

        BountyService.SortType sortType = sortTypes.getOrDefault(playerId, BountyService.SortType.HIGHEST);
        if (query == null || query.isBlank()) {
            searchQueries.remove(playerId);
            pages.put(playerId, 1);
            bountyGUI.open(player, 1, sortType, null);
            return;
        }

        searchQueries.put(playerId, query);
        pages.put(playerId, 1);
        bountyGUI.open(player, 1, sortType, query);
    }

    public boolean hasPendingAmountInput(UUID playerId) {
        return pendingActions.containsKey(playerId);
    }

    public void cancelPendingAmountInput(UUID playerId) {
        pendingActions.remove(playerId);
    }

    public void cancelChatInput(UUID playerId) {
        awaitingSearch.remove(playerId);
        pendingActions.remove(playerId);
    }

    public void applyAmountInput(Player player, String rawInput) {
        UUID playerId = player.getUniqueId();
        PendingAction pending = pendingActions.remove(playerId);
        if (pending == null) {
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(rawInput);
        } catch (NumberFormatException exception) {
            player.sendMessage(ChatUtil.message(plugin, "invalid-amount"));
            SoundUtil.playError(plugin, player);
            return;
        }

        if (pending.type() == ActionType.PLACE) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(pending.targetId());
            BountyService.PlaceResult result = plugin.getBountyService().placeBounty(player, target, amount);

            switch (result) {
                case SUCCESS -> {
                    player.sendMessage(ChatUtil.message(plugin, "bounty-placed",
                            "{amount}", plugin.getBountyService().format(amount),
                            "{target}", pending.targetName()));
                    if (plugin.getConfig().getBoolean("bounty.broadcast-on-place", true)) {
                        Bukkit.broadcast(ChatUtil.message(plugin, "broadcast-place",
                                "{player}", player.getName(),
                                "{amount}", plugin.getBountyService().format(amount),
                                "{target}", pending.targetName()));
                    }
                    SoundUtil.playSuccess(plugin, player);
                }
                case INVALID_AMOUNT -> {
                    player.sendMessage(ChatUtil.message(plugin, "invalid-amount"));
                    SoundUtil.playError(plugin, player);
                }
                case TOO_LOW -> {
                    player.sendMessage(ChatUtil.message(plugin, "amount-too-low",
                            "{min}", plugin.getBountyService().format(plugin.getBountyService().getMinAmount())));
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
        } else {
            if (!player.hasPermission("wobble.bounty.admin")) {
                player.sendMessage(ChatUtil.message(plugin, "no-permission"));
                SoundUtil.playError(plugin, player);
                return;
            }

            BountyService.SetResult result = plugin.getBountyService().setBounty(pending.targetId(), amount);
            switch (result) {
                case SUCCESS -> {
                    player.sendMessage(ChatUtil.mm("<green>Set bounty for <yellow>" + pending.targetName() + "</yellow> to <gold>"
                            + plugin.getBountyService().format(amount) + "</gold>.</green>"));
                    SoundUtil.playSuccess(plugin, player);
                }
                case INVALID_AMOUNT -> {
                    player.sendMessage(ChatUtil.message(plugin, "invalid-amount"));
                    SoundUtil.playError(plugin, player);
                }
            }
        }
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
}

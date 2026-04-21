package me.klouse.kbountyhunters.command;

import me.klouse.kbountyhunters.KBountyHunters;
import me.klouse.kbountyhunters.gui.BountyGUI;
import me.klouse.kbountyhunters.model.Bounty;
import me.klouse.kbountyhunters.service.BountyService;
import me.klouse.kbountyhunters.util.ChatUtil;
import me.klouse.kbountyhunters.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class BountyCommand implements CommandExecutor, TabCompleter {

    private final KBountyHunters plugin;
    private final BountyService bountyService;
    private final BountyGUI bountyGUI;

    public BountyCommand(KBountyHunters plugin, BountyService bountyService) {
        this.plugin = plugin;
        this.bountyService = bountyService;
        this.bountyGUI = new BountyGUI(plugin, bountyService);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtil.message(plugin, "only-player"));
            return true;
        }

        if (args.length == 0) {
            bountyGUI.open(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "place" -> handlePlace(player, args);
            case "check" -> handleCheck(player, args);
            case "top" -> handleTop(player);
            case "reload" -> handleReload(player);
            case "remove" -> handleRemove(player, args);
            case "set" -> handleSet(player, args);
            case "info" -> handleInfo(player, args);
            case "clearall" -> handleClearAll(player);
            default -> {
                player.sendMessage(ChatUtil.mm("<red>Unknown subcommand.</red>"));
                SoundUtil.playError(plugin, player);
            }
        }

        return true;
    }

    private void handlePlace(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatUtil.mm("<red>Usage: /bounty place <player> <amount></red>"));
            SoundUtil.playError(plugin, player);
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getName() == null && !target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(ChatUtil.message(plugin, "player-not-found"));
            SoundUtil.playError(plugin, player);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException exception) {
            player.sendMessage(ChatUtil.message(plugin, "invalid-amount"));
            SoundUtil.playError(plugin, player);
            return;
        }

        BountyService.PlaceResult result = bountyService.placeBounty(player, target, amount);

        switch (result) {
            case SUCCESS -> {
                player.sendMessage(ChatUtil.message(
                        plugin,
                        "bounty-placed",
                        "{amount}", bountyService.format(amount),
                        "{target}", target.getName() == null ? "Unknown" : target.getName()
                ));
                SoundUtil.playSuccess(plugin, player);

                if (plugin.getConfig().getBoolean("bounty.broadcast-on-place", true)) {
                    Bukkit.broadcast(ChatUtil.message(
                            plugin,
                            "broadcast-place",
                            "{player}", player.getName(),
                            "{amount}", bountyService.format(amount),
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
                        "{min}", bountyService.format(bountyService.getMinAmount())
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

    private void handleCheck(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatUtil.mm("<red>Usage: /bounty check <player></red>"));
            SoundUtil.playError(plugin, player);
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getName() == null && !target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(ChatUtil.message(plugin, "player-not-found"));
            SoundUtil.playError(plugin, player);
            return;
        }

        Optional<Bounty> bounty = bountyService.getBounty(target.getUniqueId());
        if (bounty.isEmpty()) {
            player.sendMessage(ChatUtil.message(
                    plugin,
                    "bounty-none",
                    "{target}", target.getName() == null ? "Unknown" : target.getName()
            ));
            SoundUtil.playError(plugin, player);
            return;
        }

        player.sendMessage(ChatUtil.message(
                plugin,
                "bounty-check",
                "{target}", target.getName() == null ? "Unknown" : target.getName(),
                "{amount}", bountyService.format(bounty.get().getAmount())
        ));
        SoundUtil.playSuccess(plugin, player);
    }

    private void handleTop(Player player) {
        List<Bounty> top = bountyService.getTopBounties(10);

        if (top.isEmpty()) {
            player.sendMessage(ChatUtil.mm("<red>No active bounties.</red>"));
            SoundUtil.playError(plugin, player);
            return;
        }

        player.sendMessage(ChatUtil.mm("<gold><bold>Top Bounties</bold></gold>"));
        int index = 1;

        for (Bounty bounty : top) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(bounty.getTargetId());
            String name = target.getName() == null ? bounty.getTargetId().toString() : target.getName();

            player.sendMessage(ChatUtil.mm(
                    "<gray>" + index + ".</gray> <yellow>" + name + "</yellow> <dark_gray>-</dark_gray> <gold>"
                            + bountyService.format(bounty.getAmount()) + "</gold>"
            ));
            index++;
        }

        SoundUtil.playSuccess(plugin, player);
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("klouse.bounty.reload")) {
            player.sendMessage(ChatUtil.message(plugin, "no-permission"));
            SoundUtil.playError(plugin, player);
            return;
        }

        plugin.reloadPlugin();
        player.sendMessage(ChatUtil.message(plugin, "reload-success"));
        SoundUtil.playSuccess(plugin, player);
    }

    private void handleRemove(Player player, String[] args) {
        if (!player.hasPermission("klouse.bounty.admin")) {
            player.sendMessage(ChatUtil.message(plugin, "no-permission"));
            SoundUtil.playError(plugin, player);
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatUtil.mm("<red>Usage: /bounty remove <player></red>"));
            SoundUtil.playError(plugin, player);
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getName() == null && !target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(ChatUtil.message(plugin, "player-not-found"));
            SoundUtil.playError(plugin, player);
            return;
        }

        BountyService.RemoveResult result = bountyService.removeBounty(target.getUniqueId());

        switch (result) {
            case SUCCESS -> {
                player.sendMessage(ChatUtil.mm("<green>Removed bounty from <yellow>" + target.getName() + "</yellow>.</green>"));
                SoundUtil.playSuccess(plugin, player);
            }
            case NOT_FOUND -> {
                player.sendMessage(ChatUtil.mm("<red>No bounty found for <yellow>" + target.getName() + "</yellow>.</red>"));
                SoundUtil.playError(plugin, player);
            }
        }
    }

    private void handleSet(Player player, String[] args) {
        if (!player.hasPermission("klouse.bounty.admin")) {
            player.sendMessage(ChatUtil.message(plugin, "no-permission"));
            SoundUtil.playError(plugin, player);
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatUtil.mm("<red>Usage: /bounty set <player> <amount></red>"));
            SoundUtil.playError(plugin, player);
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getName() == null && !target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(ChatUtil.message(plugin, "player-not-found"));
            SoundUtil.playError(plugin, player);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException exception) {
            player.sendMessage(ChatUtil.message(plugin, "invalid-amount"));
            SoundUtil.playError(plugin, player);
            return;
        }

        BountyService.SetResult result = bountyService.setBounty(target.getUniqueId(), amount);

        switch (result) {
            case SUCCESS -> {
                player.sendMessage(ChatUtil.mm("<green>Set bounty for <yellow>" + target.getName() + "</yellow> to <gold>" + bountyService.format(amount) + "</gold>.</green>"));
                SoundUtil.playSuccess(plugin, player);
            }
            case INVALID_AMOUNT -> {
                player.sendMessage(ChatUtil.message(plugin, "invalid-amount"));
                SoundUtil.playError(plugin, player);
            }
        }
    }

    private void handleInfo(Player player, String[] args) {
        if (!player.hasPermission("klouse.bounty.admin")) {
            player.sendMessage(ChatUtil.message(plugin, "no-permission"));
            SoundUtil.playError(plugin, player);
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatUtil.mm("<red>Usage: /bounty info <player></red>"));
            SoundUtil.playError(plugin, player);
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getName() == null && !target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(ChatUtil.message(plugin, "player-not-found"));
            SoundUtil.playError(plugin, player);
            return;
        }

        Optional<Bounty> optional = bountyService.getBounty(target.getUniqueId());
        if (optional.isEmpty()) {
            player.sendMessage(ChatUtil.mm("<red>No bounty found for <yellow>" + target.getName() + "</yellow>.</red>"));
            SoundUtil.playError(plugin, player);
            return;
        }

        Bounty bounty = optional.get();
        player.sendMessage(ChatUtil.mm("<dark_gray>— <gold>Admin Bounty Info</gold> <dark_gray>—"));
        player.sendMessage(ChatUtil.mm("<gray>Target:</gray> <yellow>" + target.getName() + "</yellow>"));
        player.sendMessage(ChatUtil.mm("<gray>Amount:</gray> <gold>" + bountyService.format(bounty.getAmount()) + "</gold>"));
        player.sendMessage(ChatUtil.mm("<gray>Created:</gray> <yellow>" + bounty.getCreatedAt() + "</yellow>"));
        SoundUtil.playSuccess(plugin, player);
    }

    private void handleClearAll(Player player) {
        if (!player.hasPermission("klouse.bounty.admin")) {
            player.sendMessage(ChatUtil.message(plugin, "no-permission"));
            SoundUtil.playError(plugin, player);
            return;
        }

        int removed = bountyService.clearAllBounties();
        player.sendMessage(ChatUtil.mm("<green>Cleared <gold>" + removed + "</gold> active bounties.</green>"));
        SoundUtil.playSuccess(plugin, player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.add("place");
            suggestions.add("check");
            suggestions.add("top");
            suggestions.add("reload");
            suggestions.add("remove");
            suggestions.add("set");
            suggestions.add("info");
            suggestions.add("clearall");
            return filter(suggestions, args[0]);
        }

        if (args.length == 2 && (
                args[0].equalsIgnoreCase("place")
                        || args[0].equalsIgnoreCase("check")
                        || args[0].equalsIgnoreCase("remove")
                        || args[0].equalsIgnoreCase("set")
                        || args[0].equalsIgnoreCase("info")
        )) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                suggestions.add(onlinePlayer.getName());
            }
            return filter(suggestions, args[1]);
        }

        return suggestions;
    }

    private List<String> filter(List<String> source, String input) {
        String lower = input.toLowerCase();
        return source.stream()
                .filter(value -> value.toLowerCase().startsWith(lower))
                .toList();
    }
}

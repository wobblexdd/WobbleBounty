package me.wobble.wobblebounty.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.wobble.wobblebounty.WobbleBounty;
import me.wobble.wobblebounty.util.ChatUtil;
import me.wobble.wobblebounty.util.SoundUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class PlayerSearchListener implements Listener {

    private final WobbleBounty plugin;
    private final BountyMenuListener bountyMenuListener;

    public PlayerSearchListener(WobbleBounty plugin, BountyMenuListener bountyMenuListener) {
        this.plugin = plugin;
        this.bountyMenuListener = bountyMenuListener;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        boolean waitingSearch = bountyMenuListener.isAwaitingSearch(player.getUniqueId());
        boolean waitingAmount = bountyMenuListener.hasPendingAmountInput(player.getUniqueId());

        if (!waitingSearch && !waitingAmount) {
            return;
        }

        event.setCancelled(true);
        String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (input.equalsIgnoreCase("cancel")) {
                bountyMenuListener.cancelAwaitingSearch(player.getUniqueId());
                bountyMenuListener.cancelPendingAmountInput(player.getUniqueId());
                SoundUtil.playClick(plugin, player);
                player.sendMessage(ChatUtil.mm("<gray>Action cancelled.</gray>"));
                return;
            }

            if (waitingSearch) {
                SoundUtil.playSuccess(plugin, player);
                bountyMenuListener.applySearch(player, input);
                return;
            }

            SoundUtil.playSuccess(plugin, player);
            bountyMenuListener.applyAmountInput(player, input);
        });
    }
}

package me.klouse.kbountyhunters.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.klouse.kbountyhunters.KBountyHunters;
import me.klouse.kbountyhunters.util.ChatUtil;
import me.klouse.kbountyhunters.util.SoundUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class PlayerSearchListener implements Listener {

    private final KBountyHunters plugin;
    private final BountyMenuListener bountyMenuListener;

    public PlayerSearchListener(KBountyHunters plugin, BountyMenuListener bountyMenuListener) {
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
                bountyMenuListener.cancelChatInput(player.getUniqueId());
                SoundUtil.playClick(plugin, player);
                player.sendMessage(ChatUtil.message(plugin, "action-cancelled"));
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

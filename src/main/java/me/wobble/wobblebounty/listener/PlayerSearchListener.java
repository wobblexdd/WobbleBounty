package me.wobble.wobblebounty.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.wobble.wobblebounty.WobbleBounty;
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

        if (!bountyMenuListener.hasPendingInput(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);

        String input = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(event.message())
                .trim();

        Bukkit.getScheduler().runTask(plugin, () -> bountyMenuListener.handleChatInput(player, input));
    }
}

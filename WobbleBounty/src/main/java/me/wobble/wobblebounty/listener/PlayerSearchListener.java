package me.wobble.wobblebounty.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.wobble.wobblebounty.WobbleBounty;
import me.wobble.wobblebounty.util.ChatUtil;
import me.wobble.wobblebounty.util.SoundUtil;
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

        if (!bountyMenuListener.isAwaitingSearch(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);

        String input = plainText(event);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (input.equalsIgnoreCase("cancel")) {
                bountyMenuListener.stopAwaitingSearch(player.getUniqueId());
                SoundUtil.playClick(plugin, player);
                player.sendMessage(ChatUtil.mm("<gray>Search cancelled.</gray>"));
                return;
            }

            SoundUtil.playSuccess(plugin, player);
            bountyMenuListener.applySearch(player, input);
        });
    }

    private String plainText(AsyncChatEvent event) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(event.message())
                .trim();
    }
}

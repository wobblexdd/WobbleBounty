package me.wobble.wobblebounty.listener;

import me.wobble.wobblebounty.WobbleBounty;
import me.wobble.wobblebounty.service.BountyService;
import me.wobble.wobblebounty.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class PlayerKillListener implements Listener {

    private final WobbleBounty plugin;
    private final BountyService bountyService;

    public PlayerKillListener(WobbleBounty plugin, BountyService bountyService) {
        this.plugin = plugin;
        this.bountyService = bountyService;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        Player killer = victim.getKiller();

        if (killer == null) {
            return;
        }

        double claimed = bountyService.claimBounty(killer, victim.getUniqueId());
        if (claimed <= 0.0) {
            return;
        }

        killer.sendMessage(ChatUtil.message(
                plugin,
                "bounty-received",
                "{amount}", bountyService.format(claimed),
                "{target}", victim.getName()
        ));

        if (plugin.getConfig().getBoolean("bounty.broadcast-on-claim", true)) {
            Bukkit.broadcast(ChatUtil.message(
                    plugin,
                    "broadcast-claim",
                    "{killer}", killer.getName(),
                    "{amount}", bountyService.format(claimed),
                    "{target}", victim.getName()
            ));
        }
    }
}
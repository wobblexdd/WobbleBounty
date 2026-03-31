package me.wobble.wobblebounty.listener;

import me.wobble.wobblebounty.WobbleBounty;
import me.wobble.wobblebounty.service.BountyService;
import me.wobble.wobblebounty.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerKillListener implements Listener {

    private final WobbleBounty plugin;
    private final BountyService bountyService;
    private final Map<String, Long> antiFarm = new HashMap<>();

    public PlayerKillListener(WobbleBounty plugin, BountyService bountyService) {
        this.plugin = plugin;
        this.bountyService = bountyService;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        Player killer = victim.getKiller();

        if (killer == null) {
            return;
        }

        if (killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        if (isBlockedByAntiFarm(killer.getUniqueId(), victim.getUniqueId())) {
            return;
        }

        double amount = bountyService.claimBounty(killer, victim.getUniqueId());
        if (amount <= 0.0) {
            return;
        }

        killer.sendMessage(ChatUtil.message(plugin, "bounty-received",
                "{amount}", bountyService.format(amount),
                "{target}", victim.getName()));

        if (plugin.getConfig().getBoolean("bounty.broadcast-on-claim", true)) {
            Bukkit.broadcast(ChatUtil.message(plugin, "broadcast-claim",
                    "{killer}", killer.getName(),
                    "{amount}", bountyService.format(amount),
                    "{target}", victim.getName()));
        }
    }

    private boolean isBlockedByAntiFarm(UUID killerId, UUID victimId) {
        if (!plugin.getConfig().getBoolean("anti-farm.enabled", true)) {
            return false;
        }

        long cooldownSeconds = plugin.getConfig().getLong("anti-farm.same-killer-target-cooldown-seconds", 300L);
        long now = System.currentTimeMillis();
        String key = killerId + ":" + victimId;
        Long last = antiFarm.get(key);

        if (last != null && now - last < cooldownSeconds * 1000L) {
            return true;
        }

        antiFarm.put(key, now);
        return false;
    }
}

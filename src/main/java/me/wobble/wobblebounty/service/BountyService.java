package me.wobble.wobblebounty.service;

import me.wobble.wobblebounty.WobbleBounty;
import me.wobble.wobblebounty.economy.EconomyProvider;
import me.wobble.wobblebounty.model.Bounty;
import me.wobble.wobblebounty.repository.BountyRepository;
import me.wobble.wobblebounty.util.NumberFormatUtil;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class BountyService {

    public enum SortType {
        HIGHEST,
        LOWEST
    }

    public enum PlaceResult {
        SUCCESS,
        INVALID_AMOUNT,
        TOO_LOW,
        NOT_ENOUGH_MONEY,
        SELF_TARGET
    }

    public enum RemoveResult {
        SUCCESS,
        NOT_FOUND
    }

    private final WobbleBounty plugin;
    private final EconomyProvider economyProvider;
    private final BountyRepository repository;

    public BountyService(WobbleBounty plugin, EconomyProvider economyProvider, BountyRepository repository) {
        this.plugin = plugin;
        this.economyProvider = economyProvider;
        this.repository = repository;
    }

    public boolean canPlaceOnSelf() {
        return plugin.getConfig().getBoolean("bounty.allow-self-bounty", false);
    }

    public double getMinAmount() {
        return plugin.getConfig().getDouble("bounty.min-amount", 100.0);
    }

    public double getTaxPercent() {
        return plugin.getConfig().getDouble("bounty.tax-percent", 5.0);
    }

    public String format(double amount) {
        return NumberFormatUtil.format(amount);
    }

    public Optional<Bounty> getBounty(UUID targetId) {
        return repository.findByTarget(targetId);
    }

    public List<Bounty> getTopBounties(int limit) {
        return repository.findTop(limit);
    }

    public List<Bounty> getAllBounties() {
        return repository.findAll();
    }

    public List<Bounty> getAllBounties(SortType sortType) {
        List<Bounty> list = repository.findAll();

        if (sortType == SortType.LOWEST) {
            list.sort(Comparator.comparingDouble(Bounty::getAmount));
        } else {
            list.sort(Comparator.comparingDouble(Bounty::getAmount).reversed());
        }

        return list;
    }

    public PlaceResult placeBounty(Player placer, OfflinePlayer target, double amount) {
        if (amount <= 0) {
            return PlaceResult.INVALID_AMOUNT;
        }

        if (amount < getMinAmount()) {
            return PlaceResult.TOO_LOW;
        }

        if (!canPlaceOnSelf() && placer.getUniqueId().equals(target.getUniqueId())) {
            return PlaceResult.SELF_TARGET;
        }

        double taxPercent = getTaxPercent();
        double withdrawAmount = amount + (amount * (taxPercent / 100.0));

        if (economyProvider.getEconomy().getBalance(placer) < withdrawAmount) {
            return PlaceResult.NOT_ENOUGH_MONEY;
        }

        EconomyResponse response = economyProvider.getEconomy().withdrawPlayer(placer, withdrawAmount);
        if (!response.transactionSuccess()) {
            return PlaceResult.NOT_ENOUGH_MONEY;
        }

        Bounty bounty = repository.findByTarget(target.getUniqueId())
                .orElseGet(() -> new Bounty(target.getUniqueId(), 0.0));

        bounty.addAmount(amount);
        repository.saveOrUpdate(bounty);
        repository.addContribution(target.getUniqueId(), placer.getUniqueId(), amount);

        return PlaceResult.SUCCESS;
    }

    public double claimBounty(Player killer, UUID targetId) {
        Optional<Bounty> optionalBounty = repository.findByTarget(targetId);
        if (optionalBounty.isEmpty()) {
            return 0.0;
        }

        Bounty bounty = optionalBounty.get();
        double amount = bounty.getAmount();

        EconomyResponse response = economyProvider.getEconomy().depositPlayer(killer, amount);
        if (!response.transactionSuccess()) {
            return 0.0;
        }

        boolean removeOnKill = plugin.getConfig().getBoolean("bounty.remove-bounty-on-kill", true);
        if (removeOnKill) {
            repository.delete(targetId);
        }

        return amount;
    }

    public RemoveResult removeBounty(UUID targetId) {
        Optional<Bounty> existing = repository.findByTarget(targetId);
        if (existing.isEmpty()) {
            return RemoveResult.NOT_FOUND;
        }

        repository.delete(targetId);
        return RemoveResult.SUCCESS;
    }
}

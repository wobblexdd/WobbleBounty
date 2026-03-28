package me.wobble.wobblebounty.model;

import java.util.UUID;

public final class Bounty {

    private final UUID targetId;
    private double amount;

    public Bounty(UUID targetId, double amount) {
        this.targetId = targetId;
        this.amount = amount;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void addAmount(double amount) {
        this.amount += amount;
    }
}
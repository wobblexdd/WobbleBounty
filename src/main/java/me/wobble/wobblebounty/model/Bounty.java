package me.wobble.wobblebounty.model;

import java.util.UUID;

public final class Bounty {

    private final UUID targetId;
    private double amount;
    private long createdAt;

    public Bounty(UUID targetId, double amount, long createdAt) {
        this.targetId = targetId;
        this.amount = amount;
        this.createdAt = createdAt;
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}

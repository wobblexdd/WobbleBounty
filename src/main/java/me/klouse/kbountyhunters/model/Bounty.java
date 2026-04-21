package me.klouse.kbountyhunters.model;

import java.util.UUID;

public final class Bounty {

    private final UUID targetId;
    private String targetName;
    private double amount;
    private long createdAt;

    public Bounty(UUID targetId, String targetName, double amount, long createdAt) {
        this.targetId = targetId;
        this.targetName = targetName;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = Math.max(0.0, amount);
    }

    public void addAmount(double amount) {
        this.amount += Math.max(0.0, amount);
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}

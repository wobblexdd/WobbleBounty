package me.wobble.wobblebounty.repository;

import me.wobble.wobblebounty.database.SQLiteManager;
import me.wobble.wobblebounty.model.Bounty;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class BountyRepository {

    private final SQLiteManager sqliteManager;

    public BountyRepository(SQLiteManager sqliteManager) {
        this.sqliteManager = sqliteManager;
    }

    public Optional<Bounty> findByTarget(UUID targetId) {
        String sql = "SELECT target_uuid, amount FROM bounties WHERE target_uuid = ?";

        try (PreparedStatement statement = sqliteManager.prepare(sql)) {
            statement.setString(1, targetId.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                UUID uuid = UUID.fromString(resultSet.getString("target_uuid"));
                double amount = resultSet.getDouble("amount");
                return Optional.of(new Bounty(uuid, amount));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not find bounty by target", exception);
        }
    }

    public void saveOrUpdate(Bounty bounty) {
        String sql = """
                INSERT INTO bounties (target_uuid, amount)
                VALUES (?, ?)
                ON CONFLICT(target_uuid)
                DO UPDATE SET amount = excluded.amount
                """;

        try (PreparedStatement statement = sqliteManager.prepare(sql)) {
            statement.setString(1, bounty.getTargetId().toString());
            statement.setDouble(2, bounty.getAmount());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save or update bounty", exception);
        }
    }

    public void addContribution(UUID targetId, UUID placerId, double amount) {
        String sql = "INSERT INTO bounty_contributions (target_uuid, placer_uuid, amount) VALUES (?, ?, ?)";

        try (PreparedStatement statement = sqliteManager.prepare(sql)) {
            statement.setString(1, targetId.toString());
            statement.setString(2, placerId.toString());
            statement.setDouble(3, amount);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save bounty contribution", exception);
        }
    }

    public void delete(UUID targetId) {
        String sql = "DELETE FROM bounties WHERE target_uuid = ?";

        try (PreparedStatement statement = sqliteManager.prepare(sql)) {
            statement.setString(1, targetId.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not delete bounty", exception);
        }
    }

    public List<Bounty> findTop(int limit) {
        String sql = "SELECT target_uuid, amount FROM bounties ORDER BY amount DESC LIMIT ?";
        List<Bounty> bounties = new ArrayList<>();

        try (PreparedStatement statement = sqliteManager.prepare(sql)) {
            statement.setInt(1, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    UUID uuid = UUID.fromString(resultSet.getString("target_uuid"));
                    double amount = resultSet.getDouble("amount");
                    bounties.add(new Bounty(uuid, amount));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load top bounties", exception);
        }

        bounties.sort(Comparator.comparingDouble(Bounty::getAmount).reversed());
        return bounties;
    }

    public List<Bounty> findAll() {
        String sql = "SELECT target_uuid, amount FROM bounties";
        List<Bounty> bounties = new ArrayList<>();

        try (PreparedStatement statement = sqliteManager.prepare(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("target_uuid"));
                double amount = resultSet.getDouble("amount");
                bounties.add(new Bounty(uuid, amount));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load all bounties", exception);
        }

        bounties.sort(Comparator.comparingDouble(Bounty::getAmount).reversed());
        return bounties;
    }
}
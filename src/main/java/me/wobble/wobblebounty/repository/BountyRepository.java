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
        String sql = "SELECT * FROM bounties WHERE target_uuid = ?";

        try (PreparedStatement statement = sqliteManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, targetId.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(resultSet));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load bounty", exception);
        }
    }

    public List<Bounty> findTop(int limit) {
        String sql = "SELECT * FROM bounties ORDER BY amount DESC LIMIT ?";
        List<Bounty> result = new ArrayList<>();

        try (PreparedStatement statement = sqliteManager.getConnection().prepareStatement(sql)) {
            statement.setInt(1, Math.max(1, limit));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(map(resultSet));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load top bounties", exception);
        }

        return result;
    }

    public List<Bounty> findAll() {
        String sql = "SELECT * FROM bounties";
        List<Bounty> result = new ArrayList<>();

        try (PreparedStatement statement = sqliteManager.getConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                result.add(map(resultSet));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load bounties", exception);
        }

        result.sort(Comparator.comparingDouble(Bounty::getAmount).reversed());
        return result;
    }

    public void saveOrUpdate(Bounty bounty) {
        String sql = "INSERT INTO bounties (target_uuid, target_name, amount, created_at) VALUES (?, ?, ?, ?) "
                + "ON CONFLICT(target_uuid) DO UPDATE SET target_name = excluded.target_name, amount = excluded.amount, created_at = excluded.created_at";

        try (PreparedStatement statement = sqliteManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, bounty.getTargetId().toString());
            statement.setString(2, bounty.getTargetName() == null ? "Unknown" : bounty.getTargetName());
            statement.setDouble(3, bounty.getAmount());
            statement.setLong(4, bounty.getCreatedAt());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save bounty", exception);
        }
    }

    public void addContribution(UUID targetId, UUID contributorId, double amount) {
        String sql = "INSERT INTO bounty_contributions (target_uuid, contributor_uuid, amount, created_at) VALUES (?, ?, ?, ?)";

        try (PreparedStatement statement = sqliteManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, targetId.toString());
            statement.setString(2, contributorId.toString());
            statement.setDouble(3, amount);
            statement.setLong(4, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save bounty contribution", exception);
        }
    }

    public void delete(UUID targetId) {
        String sql = "DELETE FROM bounties WHERE target_uuid = ?";

        try (PreparedStatement statement = sqliteManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, targetId.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not delete bounty", exception);
        }
    }

    public void deleteAll() {
        try (PreparedStatement statement = sqliteManager.getConnection().prepareStatement("DELETE FROM bounties")) {
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not delete all bounties", exception);
        }
    }

    private Bounty map(ResultSet resultSet) throws SQLException {
        return new Bounty(
                UUID.fromString(resultSet.getString("target_uuid")),
                resultSet.getString("target_name"),
                resultSet.getDouble("amount"),
                resultSet.getLong("created_at")
        );
    }
}

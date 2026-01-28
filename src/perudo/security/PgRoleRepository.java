package perudo.security;

import perudo.db.Db;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public final class PgRoleRepository {

    public Set<String> getRolesForAccount(long accountId) {
        String sql =
                "SELECT r.role_code " +
                        "FROM account_roles ar " +
                        "JOIN roles r ON r.id = ar.role_id " +
                        "WHERE ar.account_id = ?";

        Set<String> roles = new HashSet<>();
        try (Connection c = Db.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) roles.add(rs.getString("role_code"));
            }
            if (roles.isEmpty()) roles.add(RoleKinds.PLAYER);
            return roles;
        } catch (SQLException e) {
            throw new RuntimeException("getRolesForAccount failed: " + e.getMessage(), e);
        }
    }

    public boolean hasRole(long accountId, String roleCode) {
        return getRolesForAccount(accountId).contains(roleCode);
    }
}

package com.ratewise.security.repositories;

import com.ratewise.security.entities.Role;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RoleRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Role> roleRowMapper = (rs, rowNum) -> Role.builder()
            .id(rs.getLong("id"))
            .roleName(rs.getString("role_name"))
            .build();

    public RoleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Role> findById(Long id) {
        String sql = "SELECT id, role_name FROM roles WHERE id = ?";
        List<Role> roles = jdbcTemplate.query(sql, roleRowMapper, id);
        return roles.isEmpty() ? Optional.empty() : Optional.of(roles.get(0));
    }

    public Optional<Role> findByRoleName(String name) {
        String sql = "SELECT id, role_name FROM roles WHERE role_name = ?";
        List<Role> roles = jdbcTemplate.query(sql, roleRowMapper, name);
        return roles.isEmpty() ? Optional.empty() : Optional.of(roles.get(0));
    }

    public Optional<Role> findRoleByUserId (String userId) {
        String sql = """
                SELECT r.id, r.role_name
                FROM roles r INNER JOIN user_roles ur
                ON r.id = ur.role_id
                WHERE ur.user_id = ?
                """;
        List<Role> roles = jdbcTemplate.query(sql, roleRowMapper, userId);
        return roles.isEmpty() ? Optional.empty() : Optional.of(roles.get(0));
    }

    public void assignRoleToUser(String userId, Long roleId) {
        String deleteSql = "DELETE FROM user_roles WHERE user_id = ?";
        jdbcTemplate.update(deleteSql, userId);

        String insertSql = "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)";
        jdbcTemplate.update(insertSql, userId, roleId);
    }

    public void removeRoleFromUser(String userId) {
        String sql = "DELETE FROM user_roles WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
    }

    public List<Role> findAll() {
        String sql = "SELECT id, role_name FROM roles ORDER BY id";
        return jdbcTemplate.query(sql, roleRowMapper);
    }
}
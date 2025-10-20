package com.ratewise.security.entities;

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

    public List<Role> findRolesByUserId (Long userId) {
        String sql = """
                SELECT id, role_name
                FROM roles r INNER JOIN user_roles ur
                ON r.id = ur.role_id
                WHERE ur.user_id = ?
                """;
        return jdbcTemplate.query(sql, roleRowMapper, userId);
    }

    public void assignRoleToUser(Long userId, Long roleId) {
        String sql = "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        jdbcTemplate.update(sql, userId, roleId);
    }

    public void removeRoleFromUser(Long userId, Long roleId) {
        String sql = "DELETE FROM user_roles WHERE user_id = ? AND role_id = ?";
        jdbcTemplate.update(sql, userId, roleId);
    }
}
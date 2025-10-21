package com.ratewise.security.repositories;

import com.ratewise.security.entities.User;
import com.ratewise.security.entities.Role;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RoleRepository roleRepository;

    public UserRepository(JdbcTemplate jdbcTemplate, RoleRepository roleRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.roleRepository = roleRepository;
    }

    private final RowMapper<User> userRowMapper = new RowMapper<User>() {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            return User.builder()
                    .id(rs.getString("id"))
                    .username(rs.getString("username"))
                    .email(rs.getString("email"))
                    .password(rs.getString("password_hash"))
                    .enabled(rs.getBoolean("is_active"))
                    .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                    .build();
        }
    };

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, username);
            return Optional.ofNullable(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, email);
            return Optional.ofNullable(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<User> findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, id);
            return Optional.ofNullable(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public User save(User user) {
        if (user.getId() == null) {
            return create(user);
        } else {
            return update(user);
        }
    }

    private User create(User user) {
        String uuid = UUID.randomUUID().toString();
        String sql = """
            INSERT INTO users (id, username, password_hash, email, is_active, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(sql,
            uuid,
            user.getUsername(),
            user.getPassword(),
            user.getEmail(),
            user.isEnabled(),
            now
        );

        user.setId(uuid);
        user.setCreatedAt(now);
        return user;
    }

    private User update(User user) {
        String sql = """
            UPDATE users SET username = ?, password_hash = ?, email = ?, is_active = ?
            WHERE id = ?
            """;
        
        jdbcTemplate.update(sql, 
            user.getUsername(),
            user.getPassword(), 
            user.getEmail(), 
            user.isEnabled(), 
            user.getId()
        );
        
        return user;
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username);
        return count != null && count > 0;
    }

    public Optional<User> findByUsernameWithRole(String username) {
        Optional<User> userOpt = findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Optional<Role> role = roleRepository.findRoleByUserId(user.getId());
            role.ifPresent(user::setRole);
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public Optional<User> findByIdWithRole(String id) {
        Optional<User> userOpt = findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Optional<Role> role = roleRepository.findRoleByUserId(user.getId());
            role.ifPresent(user::setRole);
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, userRowMapper);
    }
}
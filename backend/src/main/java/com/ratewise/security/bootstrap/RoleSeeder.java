package com.ratewise.security.bootstrap;

import com.ratewise.security.entities.Role;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.ratewise.security.entities.RoleRepository;

import java.util.Optional;

@Component
public class RoleSeeder implements ApplicationListener<ContextRefreshedEvent> {
    private final RoleRepository roleRepository;
    private final JdbcTemplate jdbcTemplate;


    public RoleSeeder(RoleRepository roleRepository, JdbcTemplate jdbcTemplate) {
        this.roleRepository = roleRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        this.loadRoles();
    }

    private void loadRoles() {
        String[] roleNames = new String[] {Role.ADMIN, Role.USER};
        Long[] roleIds = new Long[] {Role.ROLE_ADMIN, Role.ROLE_USER};

        for(int i = 0; i < roleNames.length; i++) {
            String roleName = roleNames[i];
            Long roleId = roleIds[i];

            Optional<Role> optionalRole = roleRepository.findByRoleName(roleName);

            optionalRole.ifPresentOrElse(role ->
                    System.out.println("Role already exists: " + role.getRoleName()),
                    () -> {
                            String sql = "INSERT INTO roles (id, role_name) VALUES (?, ?) ON CONFLICT (id) DO NOTHING";
                            jdbcTemplate.update(sql, roleId, roleName);
                            System.out.println("Created new role: " + roleName + " with id=" + roleId);
                    });
        }
    }
}


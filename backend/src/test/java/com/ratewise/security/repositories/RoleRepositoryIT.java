package com.ratewise.security.repositories;

import com.ratewise.security.entities.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RoleRepositoryIT {
    
    @Autowired
    private RoleRepository roleRepository;

    @Test
    void testFindAll() {
        List<Role> roles = roleRepository.findAll();
        assertNotNull(roles);
        assertTrue(roles.size() > 0);
    }
    
    @Test
    void testFindByRoleName() {
        Optional<Role> user = roleRepository.findByRoleName("USER");
        Optional<Role> admin = roleRepository.findByRoleName("ADMIN");     
        assertTrue(user.isPresent());
        assertTrue(admin.isPresent());
    }
    
    @Test
    void testFindById() {
        Optional<Role> role = roleRepository.findById(1L);
        assertTrue(role.isPresent());
    }
}

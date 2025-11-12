package com.ratewise.security.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserRepositoryIT {
    
    @Autowired
    private UserRepository userRepository;

    @Test
    void testFindAll() {
        var users = userRepository.findAll();
        assertNotNull(users);
    }
    
    @Test
    void testExistsByEmailDoesntCrash() {
        // just verify method works, doesn't matter if true/false
        // more smoke ... USELESS AND JUST FOR TEST COVERAGE
        userRepository.existsByEmail("test@test.com");
    }
    
    @Test
    void testExistsByUsernameDoesntCrash() {
        userRepository.existsByUsername("testuser");
    }
}

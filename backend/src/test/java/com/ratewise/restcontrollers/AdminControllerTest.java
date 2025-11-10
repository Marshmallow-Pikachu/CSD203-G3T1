package com.ratewise.restcontrollers;

import com.ratewise.security.dto.*;
import com.ratewise.security.entities.Role;
import com.ratewise.security.entities.User;
import com.ratewise.security.exception.RoleNotFoundException;
import com.ratewise.security.exception.UserNotFoundException;
import com.ratewise.security.repositories.UserRepository;
import com.ratewise.security.repositories.RoleRepository;
import com.ratewise.security.util.JWTUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false) // define the filter as false so that i dont get 403 on everything
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RoleRepository roleRepository;

    @MockBean
    private JWTUtil jwtUtil;

    @Test
    void testGetAllUsersOk() throws Exception {
        User user = new User();
        user.setId("u1");
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setRole(new Role(1L, "ADMIN", new ArrayList<>()));

        Mockito.when(userRepository.findAll()).thenReturn(List.of(user));
        Mockito.when(userRepository.findByIdWithRole("u1")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk()) // expecting 200
                .andExpect(jsonPath("$[0].userId").value("u1"))
                .andExpect(jsonPath("$[0].role").value("ADMIN"));
    }

    @Test
    void testGetUserByIdFound() throws Exception {
        User user = new User();
        user.setId("u2");
        user.setUsername("bob");
        user.setEmail("bob@example.com");
        user.setEnabled(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setRole(new Role(2L, "USER", new ArrayList<>()));

        Mockito.when(userRepository.findByIdWithRole("u2")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/v1/admin/users/u2"))
                .andExpect(status().isOk()) // expecting 200
                .andExpect(jsonPath("$.userId").value("u2"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void testGetUserByIdNotFound() throws Exception {
        Mockito.when(userRepository.findByIdWithRole("badid")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/admin/users/badid"))
                .andExpect(status().isNotFound()); // expecting 404
    }

    @Test
    void testUpdateRoleOk() throws Exception {
        User user = new User();
        user.setId("u3");
        user.setUsername("carol");
        user.setEmail("carol@example.com");

        Role role = new Role(3L, "MODERATOR", new ArrayList<>());

        Mockito.when(userRepository.findById("u3")).thenReturn(Optional.of(user));
        Mockito.when(roleRepository.findById(3L)).thenReturn(Optional.of(role));

        mockMvc.perform(put("/api/v1/admin/users/u3/role/3"))
                .andExpect(status().isOk()) // expecting 200
                .andExpect(jsonPath("$.newRole").value("MODERATOR"))
                .andExpect(jsonPath("$.userId").value("u3"));
    }

    @Test
    void testUpdateRole_UserNotFound() throws Exception {
        Mockito.when(userRepository.findById("notfound")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/admin/users/notfound/role/1"))
                .andExpect(status().isNotFound()); // expecting 404
    }

    @Test
    void testUpdateRole_RoleNotFound() throws Exception {
        User user = new User();
        user.setId("u4");
        Mockito.when(userRepository.findById("u4")).thenReturn(Optional.of(user));
        Mockito.when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/admin/users/u4/role/99"))
                .andExpect(status().isNotFound()); // expecting 403
    } 

    @Test
    void testDisableUserStatusOk() throws Exception {
        User user = new User();
        user.setId("u5");
        user.setEnabled(false);
        Mockito.when(userRepository.findById("u5")).thenReturn(Optional.of(user));
        Mockito.when(userRepository.save(Mockito.any())).thenReturn(user);

        String body = "{\"isActive\":true}";
        mockMvc.perform(patch("/api/v1/admin/users/u5/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk()) // expecting 200
                .andExpect(jsonPath("$.userId").value("u5"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void testUpdateUserStatus_UserNotFound() throws Exception {
        Mockito.when(userRepository.findById("badid")).thenReturn(Optional.empty());

        String body = "{\"isActive\":true}";
        mockMvc.perform(patch("/api/v1/admin/users/badid/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isNotFound()); // expecting 403
    }

    @Test
    void testUpdateUserStatus_InvalidBody() throws Exception {
        User user = new User();
        user.setId("u6");
        Mockito.when(userRepository.findById("u6")).thenReturn(Optional.of(user));

        String body = "{\"notIsActive\":true}";
        mockMvc.perform(patch("/api/v1/admin/users/u6/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest()); // expecting 400
    }

    @Test
    void testGetAllRolesOk() throws Exception {
        Role role1 = new Role(1L, "ADMIN", new ArrayList<>());
        Role role2 = new Role(2L, "USER", new ArrayList<>());
        Mockito.when(roleRepository.findAll()).thenReturn(List.of(role1, role2));

        mockMvc.perform(get("/api/v1/admin/roles"))
                .andExpect(status().isOk()) // expecting 200
                .andExpect(jsonPath("$[0].roleName").value("ADMIN"))
                .andExpect(jsonPath("$[1].roleName").value("USER"));
    }
}

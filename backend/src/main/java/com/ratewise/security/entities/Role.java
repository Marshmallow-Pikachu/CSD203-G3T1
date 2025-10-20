package com.ratewise.security.entities;

import jakarta.persistence.*;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;


@Table(name = "roles")
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(name = "role_name", unique = true, nullable = false)
    private String roleName;

    public static final Long ROLE_ADMIN = 1L;
    public static final Long ROLE_USER = 2L;

    public static final String ADMIN = "ADMIN";
    public static final String USER  = "USER";
}

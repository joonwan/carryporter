package com.e101.carryporter.domain.user.entity;

import com.e101.carryporter.domain.admin.entity.AdminCredential;
import com.e101.carryporter.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(unique = true, nullable = false)
    private String mmEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private AdminCredential adminCredential;

    public static User createUser(String mmEmail) {
        return User.builder()
                .mmEmail(mmEmail)
                .role(Role.BASIC)
                .build();
    }

    public static User createAdminUser(String mmEmail, String name, String password) {
        User user = User.builder()
                .mmEmail(mmEmail)
                .role(Role.ADMIN)
                .build();

        AdminCredential adminCredential = AdminCredential.builder()
                .user(user)
                .name(name)
                .password(password)
                .build();

        user.initAdminCredential(adminCredential);
        return user;
    }

    public boolean isAdmin() {
        return this.role.equals(Role.ADMIN);
    }

    @Builder
    private User(String mmEmail, Role role) {
        this.mmEmail = mmEmail;
        this.role = role;
    }

    private void initAdminCredential(AdminCredential adminCredential) {
        if (this.adminCredential != null) {
            return;
        }

        this.adminCredential = adminCredential;
    }

}

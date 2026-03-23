package com.e101.carryporter.domain.admin.entity;


import com.e101.carryporter.domain.user.entity.User;
import com.e101.carryporter.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "admin_credentials")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminCredential extends BaseEntity {

    @Id
    @Column(name = "user_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String password;

    @Builder
    private AdminCredential(User user, String name, String password) {
        this.user = user;
        this.name = name;
        this.password = password;
    }

}

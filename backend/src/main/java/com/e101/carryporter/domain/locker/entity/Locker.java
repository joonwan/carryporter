package com.e101.carryporter.domain.locker.entity;

import com.e101.carryporter.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "lockers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Locker extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "locker_id")
    private Long id;

    @Column(unique = true, nullable = false)
    private String lockerCode;

    @Enumerated(EnumType.STRING)
    private LockerStatus lockerStatus;

    public static Locker createLocker(String lockerCode) {
        return Locker.builder()
                .lockerCode(lockerCode)
                .lockerStatus(LockerStatus.AVAILABLE)
                .build();
    }

    @Builder
    private Locker(String lockerCode, LockerStatus lockerStatus) {
        this.lockerCode = lockerCode;
        this.lockerStatus = lockerStatus;
    }

    public void updateStatus(LockerStatus status) {
        this.lockerStatus = status;
    }
}

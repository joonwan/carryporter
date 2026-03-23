package com.e101.carryporter.domain.mission.entity;

import com.e101.carryporter.domain.location.entity.Location;
import com.e101.carryporter.domain.locker.entity.Locker;
import com.e101.carryporter.domain.locker.entity.LockerStatus;
import com.e101.carryporter.domain.locker.entity.UserLockerStatus;
import com.e101.carryporter.domain.robot.entity.Robot;
import com.e101.carryporter.domain.robot.entity.RobotStatus;
import com.e101.carryporter.domain.user.entity.User;
import com.e101.carryporter.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "missions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mission_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "robot_id")
    private Robot robot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="call_location_id", nullable = false)
    private Location callLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locker_id")
    private Locker locker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MissionStatus missionStatus;

    // robot 할당시간
    private LocalDateTime robotAssignedAt;

    // robot 주행 시작시간
    private LocalDateTime startedAt;

    // 로봇 목적지 도착시간
    private LocalDateTime arrivedAt;

    // 미션 완료시간
    private LocalDateTime finishedAt;

    // 사물함 배정 시간
    private LocalDateTime lockerAssignedAt;

    // 사용자 사물함 상태
    @Enumerated(EnumType.STRING)
    private UserLockerStatus userLockerStatus;

    // 새로운 미션 생성
    public static Mission createMission(User user, Location callLocation) {
        return Mission.builder()
                .user(user)
                .callLocation(callLocation)
                .missionStatus(MissionStatus.REQUESTED)
                .userLockerStatus(UserLockerStatus.READY)
                .build();
    }

    // 로봇을 미션에 할당
    public void assignRobot(Robot robot) {
        this.robot = robot;
        this.missionStatus = MissionStatus.ASSIGNED;
        robot.changeStatus(RobotStatus.BUSY);
        this.robotAssignedAt = LocalDateTime.now();
    }

    // 미션에 사물함 배정
    public void assignLocker(Locker locker){
        this.locker = locker;
        this.lockerAssignedAt = LocalDateTime.now();
        this.userLockerStatus = UserLockerStatus.OCCUPIED;
    }

    // 로봇 주행 시작
    public void dispatch() {
        this.missionStatus = MissionStatus.MOVING;
        this.startedAt = LocalDateTime.now();
    }

    // 로봇 목적지 도착 완료
    public void arrive() {
        this.missionStatus = MissionStatus.ARRIVED;
        this.arrivedAt = LocalDateTime.now();
    }

    // 로봇 복귀 완료
    public void returned() {
        this.missionStatus = MissionStatus.RETURNED;
        this.arrivedAt = LocalDateTime.now();
    }

    // 미션 종료
    public void finish() {
        this.missionStatus = MissionStatus.FINISHED;
        this.userLockerStatus = UserLockerStatus.COMPLETED;
        this.finishedAt = LocalDateTime.now();
    }

    // 미션 보관
    public void store() {
        this.missionStatus = MissionStatus.STORING;
    }

    @Builder
    private Mission(User user, Location callLocation, MissionStatus missionStatus, UserLockerStatus userLockerStatus) {
        this.user = user;
        this.callLocation = callLocation;
        this.missionStatus = missionStatus;
        this.userLockerStatus = userLockerStatus;
    }

    public void failed() {
        this.missionStatus = MissionStatus.FAILED;
    }

    public void lock() {
        this.missionStatus = MissionStatus.LOCKED;
    }

    public void unlock() {
        this.missionStatus = MissionStatus.UNLOCKED;
    }

    public void returning() {
        this.missionStatus = MissionStatus.RETURNING;
    }

    public void updateLocation(Location newLocation) {
        this.callLocation = newLocation;
    }
}

package com.e101.carryporter.domain.mission.repository;

import com.e101.carryporter.domain.mission.entity.Mission;
import com.e101.carryporter.domain.mission.entity.MissionStatus;
import com.e101.carryporter.domain.robot.entity.Robot;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MissionRepository {

    private final EntityManager em;

    public Long save(Mission mission) {
        em.persist(mission);
        return mission.getId();
    }

    public Optional<Mission> findById(Long missionId) {
        return Optional.ofNullable(em.find(Mission.class, missionId));
    }

    /**
     * 로봇의 특정 상태인 미션 조회
     */
    public Optional<Mission> findByRobotAndStatus(Robot robot, MissionStatus status) {
        List<Mission> results = em.createQuery(
                        "SELECT m FROM Mission m WHERE m.robot = :robot AND m.missionStatus = :status",
                        Mission.class)
                .setParameter("robot", robot)
                .setParameter("status", status)
                .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * MAC 주소로 여러 상태 중 하나에 해당하는 미션 조회
     */
    public Optional<Mission> findByMacAddressAndStatusIn(String macAddress, List<MissionStatus> statuses) {
        List<Mission> results = em.createQuery(
                        "SELECT m FROM Mission m JOIN m.robot r WHERE r.macAddress = :macAddress AND m.missionStatus IN :statuses",
                        Mission.class)
                .setParameter("macAddress", macAddress)
                .setParameter("statuses", statuses)
                .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * MAC 주소로 특정 상태인 미션 조회 (같은 MAC을 가진 모든 로봇 포함)
     */
    public Optional<Mission> findByMacAddressAndStatus(String macAddress, MissionStatus status) {
        List<Mission> results = em.createQuery(
                        "SELECT m FROM Mission m JOIN m.robot r WHERE r.macAddress = :macAddress AND m.missionStatus = :status",
                        Mission.class)
                .setParameter("macAddress", macAddress)
                .setParameter("status", status)
                .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * 로봇의 진행 중인 미션 조회 (FINISHED 제외)
     */
    public Optional<Mission> findActiveByRobot(Robot robot) {
        List<Mission> results = em.createQuery(
                        "SELECT m FROM Mission m WHERE m.robot = :robot AND m.missionStatus != :finished ORDER BY m.createdAt DESC",
                        Mission.class)
                .setParameter("robot", robot)
                .setParameter("finished", MissionStatus.FINISHED)
                .setMaxResults(1)
                .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * 최근 미션 조회 (최대 limit 개수)
     */
    public List<Mission> findAllWithLimit(int limit) {
        return em.createQuery(
                        "SELECT m FROM Mission m ORDER BY m.createdAt DESC",
                        Mission.class)
                .setMaxResults(limit)
                .getResultList();
    }

    public List<Mission> findByUserId(Long userId) {
        return new ArrayList<>(em.createQuery("select m from Mission m where m.user.id = :userId", Mission.class)
                .setParameter("userId", userId)
                .getResultList());
    }

    public Optional<Mission> findByUserIdAndMissionStatus(Long userId, MissionStatus missionStatus) {
        return em.createQuery("select m from Mission m where m.user.id = :userId and m.missionStatus = :missionStatus", Mission.class)
                .setParameter("userId", userId)
                .setParameter("missionStatus", missionStatus)
                .getResultList()
                .stream()
                .findAny();
    }

    public void failAllExceptFinished() {
        em.createQuery("select m from Mission m where m.missionStatus != :finished", Mission.class)
                .setParameter("finished", MissionStatus.FINISHED)
                .getResultList()
                .forEach(Mission::failed);
    }
}

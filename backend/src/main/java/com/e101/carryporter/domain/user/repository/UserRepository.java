package com.e101.carryporter.domain.user.repository;

import com.e101.carryporter.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final EntityManager em;

    public Long save(User user) {
        em.persist(user);
        return user.getId();
    }

    public Optional<User> findById(Long userId) {
        return Optional.ofNullable(em.find(User.class, userId));
    }

    public Optional<User> findByMmEmail(String mmEmail) {
        // 1. JPQL 쿼리 작성 (select u from User u where u.mmEmail = :mmEmail)
        List<User> result = em.createQuery("select u from User u where u.mmEmail = :mmEmail", User.class)
                .setParameter("mmEmail", mmEmail)
                .getResultList();

        // 2. 결과가 있으면 첫 번째 것 반환, 없으면 Optional.empty() 반환
        return result.stream().findAny();
    }

    public Optional<User> findByMmEmailWithAdminCredential(String email) {
        return em.createQuery("select u from User u join fetch u.adminCredential where u.mmEmail = :email", User.class)
                .setParameter("email", email)
                .getResultList().stream().findAny();
    }

    public long count() {
        return em.createQuery("select count(u) from User u", Long.class)
                .getSingleResult();
    }
}

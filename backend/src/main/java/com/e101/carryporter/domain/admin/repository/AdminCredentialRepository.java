package com.e101.carryporter.domain.admin.repository;

import com.e101.carryporter.domain.admin.entity.AdminCredential;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AdminCredentialRepository {

    private final EntityManager em;

    public Optional<AdminCredential> findByName(String name) {
        List<AdminCredential> result = em.createQuery(
                        "select ac from AdminCredential ac where ac.name = :name", AdminCredential.class)
                .setParameter("name", name)
                .getResultList();

        return result.stream().findAny();
    }
}

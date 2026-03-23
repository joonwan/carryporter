package com.e101.carryporter.domain.ticket.repository;

import com.e101.carryporter.domain.ticket.entity.Ticket;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TicketRepository {

    private final EntityManager em;

    public Long save(Ticket ticket) {
        em.persist(ticket);
        return ticket.getId();
    }

    public Optional<Ticket> findById(Long ticketId) {
        return Optional.ofNullable(em.find(Ticket.class, ticketId));
    }

    public Optional<Ticket> findLatestByUserId(Long userId) {
        return em.createQuery(
                "SELECT t FROM Ticket t WHERE t.user.id = :userId ORDER BY t.createdAt DESC",
                        Ticket.class)
                .setParameter("userId", userId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }
}

package com.e101.carryporter.domain.ticket.repository;

import com.e101.carryporter.domain.ticket.entity.Ticket;
import com.e101.carryporter.domain.user.entity.User;
import com.e101.carryporter.domain.user.repository.UserRepository;
import com.e101.carryporter.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TicketRepositoryTest extends IntegrationTestSupport {

    @Autowired
    TicketRepository ticketRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EntityManager em;

    @DisplayName("티켓을 저장할 수 있다.")
    @Test
    void saveTicket() {
        // given
        User user = User.createUser("test@mm.com");
        userRepository.save(user);

        Ticket ticket = Ticket.builder()
                .user(user)
                .flightNo("KE123")
                .gate("A12")
                .boardingTime(LocalDateTime.of(2025, 1, 15, 10, 30))
                .build();

        // when
        Long savedId = ticketRepository.save(ticket);
        flushAndClear();

        Ticket findTicket = ticketRepository.findById(savedId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));

        // then
        assertThat(findTicket.getFlightNo()).isEqualTo(ticket.getFlightNo());
        assertThat(findTicket.getGate()).isEqualTo(ticket.getGate());
        assertThat(findTicket.getBoardingTime()).isEqualTo(ticket.getBoardingTime());
        assertThat(findTicket.getUser().getId()).isEqualTo(user.getId());
    }

    @DisplayName("존재하지 않는 티켓의 pk 로 조회시 빈 옵셔널이 반환된다")
    @Test
    void findByNotExistId() {
        // given
        Long notExistTicketId = 99999L;

        // when
        Optional<Ticket> ticketOpt = ticketRepository.findById(notExistTicketId);

        // then
        assertThat(ticketOpt).isEmpty();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

}

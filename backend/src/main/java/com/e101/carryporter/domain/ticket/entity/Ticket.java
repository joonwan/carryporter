package com.e101.carryporter.domain.ticket.entity;

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
@Table(name = "tickets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 항공편
    @Column(nullable = false)
    private String flightNo;

    // 게이트명
    @Column(nullable = false)
    private String gate;

    // 보딩 시간
    @Column(nullable = false)
    private LocalDateTime boardingTime;

    @Builder
    private Ticket(User user, String flightNo, String gate, LocalDateTime boardingTime) {
        this.user = user;
        this.flightNo = flightNo;
        this.gate = gate;
        this.boardingTime = boardingTime;
    }
}

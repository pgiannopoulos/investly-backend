package com.investly.app.dao;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "response")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class ResponseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "response_seq")
    @SequenceGenerator(name = "response_seq", sequenceName = "response_seq", allocationSize = 1)
    private Long id;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "timestamp", columnDefinition = "TIMESTAMP WITH TIME ZONE", nullable = false)
    private OffsetDateTime timestamp;

    @OneToOne
    @JoinColumn(name = "message_id", nullable = false)
    private MessageEntity messageEntity;
}

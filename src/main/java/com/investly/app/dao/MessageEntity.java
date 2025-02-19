package com.investly.app.dao;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "messages")
@RequiredArgsConstructor
@Getter
@Setter
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String textPrompt;

    @Column(nullable = false)
    private OffsetDateTime timestamp;

    @Column(name = "thread_id")
    private String threadId;
}

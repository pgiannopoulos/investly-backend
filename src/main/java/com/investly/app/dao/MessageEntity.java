package com.investly.app.dao;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;


@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne  //because different message entries can have the same mask_id
    @JoinColumn(name = "mask_id", nullable = false) //foreign_key reference
    private MaskEntity maskEntity;

    @Column(name = "text_prompt", columnDefinition = "TEXT", nullable = false)
    private String textPrompt;

    @Column(name = "timestamp", columnDefinition = "TIMESTAMP WITH TIME ZONE", nullable = false)
    private OffsetDateTime timestamp;


}

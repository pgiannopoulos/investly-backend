package com.investly.app.dao;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@NoArgsConstructor
@Table(name = "messages")
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "mask_id", nullable = false)
    private MaskEntity maskEntity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String textPrompt;

    @Column(nullable = false)
    private OffsetDateTime timestamp;

    @Column(name = "thread_id")
    private String threadId;

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MaskEntity getMaskEntity() {
        return maskEntity;
    }

    public void setMaskEntity(MaskEntity maskEntity) {
        this.maskEntity = maskEntity;
    }

    public String getTextPrompt() {
        return textPrompt;
    }

    public void setTextPrompt(String textPrompt) {
        this.textPrompt = textPrompt;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public OffsetDateTime setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
        return timestamp;
    }

    public Integer getMaskId(){
        return maskEntity != null ? maskEntity.getId() : null;
    }
}

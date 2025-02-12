package com.investly.app.dao;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

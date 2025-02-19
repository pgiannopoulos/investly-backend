package com.investly.app.dao;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "response")
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

    @Column(name = "thread_id")
    private String threadId;

    public ResponseEntity(MessageEntity messageEntity, String aiResponse) {
        this.messageEntity = messageEntity;
        this.message = aiResponse;
        this.timestamp = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public MessageEntity getMessageEntity() {
        return messageEntity;
    }

    public void setMessageEntity(MessageEntity messageEntity) {
        this.messageEntity = messageEntity;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public void setMessageId(Long messageId) {
        this.messageEntity = new MessageEntity();
        this.messageEntity.setId(messageId);
    }

}

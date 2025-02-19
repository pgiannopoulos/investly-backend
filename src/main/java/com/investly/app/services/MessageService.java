package com.investly.app.services;

import com.investly.app.dao.MessageEntity;
import com.investly.app.dao.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;


import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    @Autowired
    private AIService aiService;

    public MessageEntity createMessage(String userMessage) throws IOException {
        String threadId = getLatestOpenThread();

        if (threadId == null) {
            threadId = aiService.createThread();
        }

        MessageEntity message = new MessageEntity();
        message.setTextPrompt(userMessage);
        message.setTimestamp(OffsetDateTime.now());
        message.setThreadId(threadId);

        messageRepository.save(message);
        return message;
    }


    public String processMessage(String userMessage) {
        return aiService.processUserMessage(userMessage);
    }

    private String getLatestOpenThread() {
        Optional<MessageEntity> lastMessage = messageRepository.findFirstByOrderByTimestampDesc();
        return lastMessage.map(MessageEntity::getThreadId).orElse(null);
    }

}

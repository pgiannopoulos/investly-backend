package com.investly.app.services;

import com.investly.app.dao.MaskEntity;
import com.investly.app.dao.MaskRepository;
import com.investly.app.dao.MessageEntity;
import com.investly.app.dao.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;


import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final MaskRepository maskRepository;

    @Autowired
    private AIService aiService;

    public MessageEntity createMessage(Integer maskId, String textPrompt) {
        String threadId = getOpenThreadForMask(maskId);

        if (threadId == null) {
            try {
                threadId = aiService.createThread(); // Create new thread if none exists
            } catch (IOException e) {
                throw new RuntimeException("Failed to create a new thread", e);
            }
        }

        MessageEntity message = new MessageEntity();
        MaskEntity maskEntity = maskRepository.findById(maskId)
                .orElseThrow(() -> new RuntimeException("Mask not found"));
        message.setMaskEntity(maskEntity);

        message.setTextPrompt(textPrompt);
        message.setThreadId(threadId); // Associate message with thread
        message.setTimestamp(OffsetDateTime.now());
        return messageRepository.save(message);
    }

    public String processMessage(Integer maskId, String userMessage) {
        return aiService.processUserMessage(maskId, userMessage);
    }

    public String getOpenThreadForMask(Integer maskId) {
        MaskEntity maskEntity = maskRepository.findById(maskId).orElse(null);
        if (maskEntity == null) {
            return null; // No mask entity found
        }

        Optional<MessageEntity> lastMessageOptional = messageRepository.findFirstByMaskEntityOrderByTimestampDesc(maskEntity);

        if (lastMessageOptional.isPresent()) {
            MessageEntity lastMessage = lastMessageOptional.get();
            return lastMessage.getThreadId(); // Reuse existing thread
        }

        return null; // No open thread found
    }


}

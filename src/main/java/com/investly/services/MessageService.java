package com.investly.services;

import com.investly.entities.MaskEntity;
import com.investly.entities.MessageEntity;
import com.investly.repositories.MaskRepository;
import com.investly.repositories.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final MaskRepository maskRepository;

    @Autowired
    public MessageService(MessageRepository messageRepository, MaskRepository maskRepository) {
        this.messageRepository = messageRepository;
        this.maskRepository = maskRepository;
    }

    public MessageEntity createMessage(Integer maskId, String textPrompt) {
        MaskEntity mask = maskRepository.findById(maskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mask not found"));

        MessageEntity message = new MessageEntity();
        message.setMaskEntity(mask);
        message.setTextPrompt(textPrompt);
        message.setTimestamp(OffsetDateTime.now());

        return messageRepository.save(message);
    }
}

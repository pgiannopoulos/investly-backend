package com.investly.app.services;

import com.investly.app.dao.MaskEntity;
import com.investly.app.dao.MessageEntity;
import com.investly.app.dao.MaskRepository;
import com.investly.app.dao.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final MaskRepository maskRepository;


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

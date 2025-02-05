package com.investly.app;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageRepository messageRepository;
    private final MaskRepository maskRepository;

    @PostMapping("/new")
    @Transactional
    public ResponseEntity<MessageEntity> createMessage(@RequestBody MessageEntity messageEntity) {
        if (messageEntity.getMaskEntity() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MaskEntity is required");
        }

        MaskEntity maskEntity = maskRepository.findById(messageEntity.getMaskEntity().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mask entity with this ID not found"));

        messageEntity.setMaskEntity(maskEntity);
        messageEntity.setTimestamp(OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.CREATED).body(messageRepository.save(messageEntity));
    }

}



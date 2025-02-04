package com.investly.app;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageRepository messageRepository;
    private final MaskRepository maskRepository;

    @PostMapping("/new")
    public ResponseEntity<MessageEntity> createMessage(@RequestBody MessageEntity messageEntity) {
        if (messageEntity.getMaskEntity() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MaskEntity is required");
        }

        //used valueOf because it needed a string and we provided an int
        MaskEntity maskEntity = maskRepository.findById(String.valueOf(messageEntity.getMaskEntity().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mask entity with this ID not found"));

        messageEntity.setMaskEntity(maskEntity);
        messageEntity.setTimestamp(OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.CREATED).body(messageRepository.save(messageEntity));
    }

}



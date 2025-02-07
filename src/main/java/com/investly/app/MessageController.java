package com.investly.app;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;

    @Autowired
    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/new")
    public ResponseEntity<MessageEntity> createMessage(@RequestBody MessageRequest messageRequest) {
        MessageEntity savedMessage = messageService.createMessage(messageRequest.getMaskId(), messageRequest.getTextPrompt());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedMessage);
    }
}





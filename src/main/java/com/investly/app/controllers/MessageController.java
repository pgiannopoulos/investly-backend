package com.investly.app.controllers;

import com.investly.app.dao.MessageEntity;
import com.investly.app.dto.MessageRequest;
import com.investly.app.services.MessageService;
import com.investly.app.services.ResponseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;
    private final ResponseService responseService;

    @Autowired
    public MessageController(MessageService messageService, ResponseService responseService) {
        this.messageService = messageService;
        this.responseService = responseService;
    }

    @PostMapping("/new")
    public ResponseEntity<Map<String, Object>> createMessage(@RequestBody MessageRequest messageRequest) {
        MessageEntity savedMessage = messageService.createMessage(messageRequest.getMaskId(), messageRequest.getTextPrompt());

        // Retrieve the AI response for this message
        com.investly.app.dao.ResponseEntity aiResponse = responseService.getResponseByMessageId(savedMessage.getId());


        // Return both message and AI response
        Map<String, Object> response = new HashMap<>();
        response.put("message", savedMessage);
        response.put("aiResponse", aiResponse);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

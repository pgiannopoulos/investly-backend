package com.investly.app.controllers;

import com.investly.app.dao.MessageEntity;
import com.investly.app.dao.ResponseEntity; // ✅ Import database entity
import com.investly.app.dto.MessageRequest;
import com.investly.app.services.AIService;
import com.investly.app.services.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AIController {

    private final AIService aiService;
    private final MessageService messageService;

    @Autowired
    public AIController(AIService aiService, MessageService messageService) {
        this.aiService = aiService;
        this.messageService = messageService;
    }

    @PostMapping("/process")
    public org.springframework.http.ResponseEntity<Map<String, Object>> processMessage(@RequestBody MessageRequest messageRequest) {
        MessageEntity savedMessage = messageService.createMessage(messageRequest.getMaskId(), messageRequest.getTextPrompt());

        com.investly.app.dao.ResponseEntity aiResponse = aiService.generateAndSaveAIResponse(savedMessage);

        Map<String, Object> response = new HashMap<>();
        response.put("message", savedMessage);
        response.put("aiResponse", aiResponse);

        return org.springframework.http.ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

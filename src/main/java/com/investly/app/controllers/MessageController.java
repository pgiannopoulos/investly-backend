package com.investly.app.controllers;

import com.investly.app.dao.MessageEntity;
import com.investly.app.dto.MessageRequest;
import com.investly.app.services.MessageService;
import com.investly.app.services.ResponseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@MessageMapping("/messages")
@SendTo("/topic/messages")
public class MessageController {

    private final MessageService messageService;
    private final ResponseService responseService;

    @Autowired
    public MessageController(MessageService messageService, ResponseService responseService) {
        this.messageService = messageService;
        this.responseService = responseService;
    }

    @MessageMapping("/new")
    public Map<String, Object> createMessage(@Payload MessageRequest messageRequest) throws IOException {
        // Persist user message
        MessageEntity savedMessage = messageService.createMessage(messageRequest.getTextPrompt());

        // Process AI response (pass only the user message)
        String aiResponseText = messageService.processMessage(savedMessage.getTextPrompt());

        // Persist AI response
        com.investly.app.dao.ResponseEntity aiResponse = new com.investly.app.dao.ResponseEntity();
        aiResponse.setMessage(aiResponseText);
        aiResponse.setMessageId(savedMessage.getId());
        aiResponse.setTimestamp(OffsetDateTime.now());
        aiResponse.setThreadId(savedMessage.getThreadId());

        // Save the AI response to the database
        responseService.saveResponse(aiResponse);

        // Return both user message and AI response
        Map<String, Object> response = new HashMap<>();
        response.put("userMessage", savedMessage);
        response.put("aiResponse", aiResponse);

        return response;
    }
}

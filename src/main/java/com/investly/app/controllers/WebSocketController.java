package com.investly.app.controllers;

import com.investly.app.dao.MessageEntity;
import com.investly.app.dto.MessageRequest;
import com.investly.app.services.MessageService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.io.IOException;

@Controller
public class WebSocketController {
    private final MessageService messageService;

    public WebSocketController(MessageService messageService) {
        this.messageService = messageService;
    }

    @MessageMapping("/send")
    @SendTo("/topic/messages")
    public MessageEntity processMessage(MessageRequest messageRequest) throws IOException {
        return messageService.createMessage(messageRequest.getTextPrompt());
    }
}

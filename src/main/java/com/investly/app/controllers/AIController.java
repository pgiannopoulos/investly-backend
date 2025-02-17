package com.investly.app.controllers;

import com.investly.app.services.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AIController {

    private final AIService aiService;

    @Autowired
    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, String>> processMessage(@RequestBody Map<String, Object> request) {
        Integer maskId = (Integer) request.get("maskId");
        String userMessage = (String) request.get("message");

        String aiResponse = aiService.processUserMessage(maskId, userMessage);

        return ResponseEntity.ok(Map.of("response", aiResponse));
    }
}

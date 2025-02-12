package com.investly.app.services;

import com.investly.app.dao.MessageEntity;
import com.investly.app.dao.ResponseEntity;
import com.investly.app.dao.ResponseRepository;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class AIService {

    private final OpenAiService openAiService;
    private final ResponseRepository responseRepository;

    public AIService(@Value("${openai.api.key}") String openAiApiKey, ResponseRepository responseRepository) {
        this.openAiService = new OpenAiService(openAiApiKey, Duration.ofSeconds(30));
        this.responseRepository = responseRepository;
    }

    public ResponseEntity generateAndSaveAIResponse(MessageEntity messageEntity) {
        ChatCompletionRequest chatRequest = ChatCompletionRequest.builder()
                .model("gpt-4")
                .messages(List.of(
                        new ChatMessage("system", "You are a financial investment assistant."),
                        new ChatMessage("user", messageEntity.getTextPrompt())
                ))
                .temperature(0.7)
                .maxTokens(200)
                .build();

        ChatCompletionResult result = openAiService.createChatCompletion(chatRequest);
        String aiResponse = result.getChoices().get(0).getMessage().getContent().trim();

        // ✅ Save AI response in the database
        ResponseEntity responseEntity = new ResponseEntity(messageEntity, aiResponse);
        return responseRepository.save(responseEntity);
    }
}

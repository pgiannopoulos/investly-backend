package com.investly.app.dto;

import lombok.Data;

@Data
public class MessageRequest {
    private String textPrompt;

    public String getTextPrompt() {
        return textPrompt;
    }

    public void setTextPrompt(String textPrompt) {
        this.textPrompt = textPrompt;
    }
}


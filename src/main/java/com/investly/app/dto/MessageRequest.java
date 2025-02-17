package com.investly.app.dto;

import lombok.Data;

@Data
public class MessageRequest {
    private Integer maskId;
    private String textPrompt;

    public Integer getMaskId() {
        return maskId;
    }
    public String getTextPrompt() {
        return textPrompt;
    }

}

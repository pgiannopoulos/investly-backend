package com.investly.dto;

import lombok.Data;

@Data
public class MessageRequest {
    private Integer maskId;
    private String textPrompt;
}

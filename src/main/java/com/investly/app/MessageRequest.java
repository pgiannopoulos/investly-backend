package com.investly.app;

import lombok.Data;

@Data
public class MessageRequest {
    private Integer maskId;
    private String textPrompt;
}

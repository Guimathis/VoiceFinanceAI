package com.voicefinance.controller;

import com.voicefinance.domain.dto.ChatResponse;
import com.voicefinance.domain.dto.ChatRequest;
import com.voicefinance.ai.client.ExpenseClient;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController(value = "/action")
public class ChatController {

    private final ExpenseClient expenseClient;

    public ChatController(ExpenseClient expenseClient) {
        this.expenseClient = expenseClient;
    }

    @PostMapping(value = "/")
    public ResponseEntity<ChatResponse> action(@RequestBody @Valid ChatRequest request) {
        ChatResponse chatResponseDTO = expenseClient.executeAction(request);
        return ResponseEntity.ok().body(chatResponseDTO);
    }
}

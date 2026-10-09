package com.voicefinance.controller;

import com.voicefinance.ai.client.ExpenseClient;
import com.voicefinance.domain.dto.ChatRequest;
import com.voicefinance.domain.dto.ChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chat com IA", description = "Endpoints para interação conversacional com o agente de IA")
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ExpenseClient expenseClient;

    @Operation(summary = "Processar mensagem em linguagem natural", description = "Envia comando de texto para o agente interpretar, criar despesas ou esclarecer dúvidas.")
    @PostMapping
    public ResponseEntity<ChatResponse> action(@RequestBody @Valid ChatRequest request) {
        ChatResponse chatResponseDTO = expenseClient.executeAction(request);
        return ResponseEntity.ok(chatResponseDTO);
    }
}

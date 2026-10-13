package com.voicefinance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicefinance.ai.client.ExpenseClient;
import com.voicefinance.domain.dto.ChatRequest;
import com.voicefinance.domain.dto.ChatResponse;
import com.voicefinance.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ExpenseClient expenseClient;

    @InjectMocks
    private ChatController chatController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chatController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/v1/chat - Deve enviar comando para o agente e retornar resposta")
    void shouldSendMessageToAgent() throws Exception {
        ChatRequest request = new ChatRequest("Gastei 45 reais no almoço");
        ChatResponse expectedResponse = new ChatResponse("Anotei R$ 45,00 em Alimentação. Confirma o registro?");

        when(expenseClient.executeAction(any(ChatRequest.class))).thenReturn(expectedResponse);

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Anotei R$ 45,00 em Alimentação. Confirma o registro?"));
    }

    @Test
    @DisplayName("POST /api/v1/chat - Deve retornar 400 Bad Request se a mensagem estiver em branco")
    void shouldReturnBadRequestWhenActionIsBlank() throws Exception {
        ChatRequest invalidRequest = new ChatRequest("");

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }
}

package com.voicefinance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voicefinance.domain.commands.ListExpensesCommand;
import com.voicefinance.domain.dto.CreateExpenseRequest;
import com.voicefinance.domain.dto.ExpenseResponse;
import com.voicefinance.domain.dto.ExpenseSummaryResponse;
import com.voicefinance.domain.dto.UpdateExpenseRequest;
import com.voicefinance.domain.enums.Category;
import com.voicefinance.domain.enums.ExpenseOrigin;
import com.voicefinance.domain.enums.ExpenseStatus;
import com.voicefinance.exception.ExpenseNotEditableException;
import com.voicefinance.exception.ExpenseNotFoundException;
import com.voicefinance.exception.GlobalExceptionHandler;
import com.voicefinance.service.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ExpenseControllerTest {

    @Mock
    private ExpenseService expenseService;

    @InjectMocks
    private ExpenseController expenseController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID sampleId;
    private ExpenseResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(expenseController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleId = UUID.randomUUID();
        sampleResponse = new ExpenseResponse(
                sampleId,
                new BigDecimal("45.50"),
                "BRL",
                Category.ALIMENTACAO,
                "Lanche da tarde",
                LocalDate.of(2026, 10, 9),
                ExpenseStatus.CONFIRMED,
                ExpenseOrigin.MANUAL,
                Instant.now(),
                Instant.now(),
                List.of()
        );
    }

    @Test
    @DisplayName("POST /api/v1/expenses - Deve criar despesa manual e retornar 201 Created")
    void shouldCreateExpenseSuccessfully() throws Exception {
        CreateExpenseRequest request = new CreateExpenseRequest(
                new BigDecimal("45.50"),
                "BRL",
                Category.ALIMENTACAO,
                "Lanche da tarde",
                LocalDate.of(2026, 10, 9)
        );

        when(expenseService.createManualExpense(any(CreateExpenseRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(sampleId.toString()))
                .andExpect(jsonPath("$.amount").value(45.50))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("POST /api/v1/expenses - Deve retornar 400 Bad Request quando valor for nulo ou inválido")
    void shouldReturnBadRequestWhenValidationFails() throws Exception {
        CreateExpenseRequest invalidRequest = new CreateExpenseRequest(
                null,
                "BRL",
                null,
                "Sem valor e sem categoria",
                null
        );

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("GET /api/v1/expenses/{id} - Deve retornar 200 OK quando despesa existir")
    void shouldGetExpenseById() throws Exception {
        when(expenseService.getExpenseById(sampleId)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/expenses/{id}", sampleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleId.toString()))
                .andExpect(jsonPath("$.description").value("Lanche da tarde"));
    }

    @Test
    @DisplayName("GET /api/v1/expenses/{id} - Deve retornar 404 Not Found quando não existir")
    void shouldReturnNotFoundWhenExpenseDoesNotExist() throws Exception {
        when(expenseService.getExpenseById(sampleId)).thenThrow(new ExpenseNotFoundException(sampleId));

        mockMvc.perform(get("/api/v1/expenses/{id}", sampleId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Expense Not Found"));
    }

    @Test
    @DisplayName("GET /api/v1/expenses - Deve retornar página de despesas")
    void shouldListExpensesPaginated() throws Exception {
        when(expenseService.findExpensesPaginated(any(ListExpensesCommand.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleResponse)));

        mockMvc.perform(get("/api/v1/expenses")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(sampleId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/expenses/{id} - Deve atualizar e retornar 200 OK")
    void shouldUpdateExpense() throws Exception {
        UpdateExpenseRequest updateRequest = new UpdateExpenseRequest(
                new BigDecimal("50.00"),
                "BRL",
                Category.ALIMENTACAO,
                "Lanche combo",
                LocalDate.of(2026, 10, 9)
        );

        when(expenseService.updateExpense(eq(sampleId), any(UpdateExpenseRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(put("/api/v1/expenses/{id}", sampleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/expenses/{id} - Deve retornar 409 Conflict se não for editável")
    void shouldReturnConflictWhenExpenseNotEditable() throws Exception {
        UpdateExpenseRequest updateRequest = new UpdateExpenseRequest(
                new BigDecimal("50.00"), null, null, null, null
        );

        when(expenseService.updateExpense(eq(sampleId), any(UpdateExpenseRequest.class)))
                .thenThrow(new ExpenseNotEditableException(ExpenseStatus.CONFIRMED));

        mockMvc.perform(put("/api/v1/expenses/{id}", sampleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Expense Not Editable"));
    }

    @Test
    @DisplayName("POST /api/v1/expenses/{id}/confirm - Deve confirmar despesa e retornar 200 OK")
    void shouldConfirmExpenseDirect() throws Exception {
        when(expenseService.confirmExpenseDirect(sampleId)).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/expenses/{id}/confirm", sampleId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/expenses/{id}/cancel - Deve cancelar despesa e retornar 200 OK")
    void shouldCancelExpenseDirect() throws Exception {
        when(expenseService.cancelExpenseDirect(sampleId)).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/expenses/{id}/cancel", sampleId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/expenses/{id} - Deve excluir despesa e retornar 204 No Content")
    void shouldDeleteExpense() throws Exception {
        doNothing().when(expenseService).deleteExpense(sampleId);

        mockMvc.perform(delete("/api/v1/expenses/{id}", sampleId))
                .andExpect(status().isNoContent());

        verify(expenseService).deleteExpense(sampleId);
    }

    @Test
    @DisplayName("GET /api/v1/expenses/summary - Deve retornar resumo financeiro")
    void shouldGetSummary() throws Exception {
        ExpenseSummaryResponse summary = new ExpenseSummaryResponse(
                new BigDecimal("250.00"),
                new BigDecimal("50.00"),
                5,
                3,
                1,
                1,
                List.of(new ExpenseSummaryResponse.CategorySummary(Category.ALIMENTACAO, new BigDecimal("250.00"), 3, 100.0))
        );

        when(expenseService.getExpenseSummary(any(), any())).thenReturn(summary);

        mockMvc.perform(get("/api/v1/expenses/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSpent").value(250.00))
                .andExpect(jsonPath("$.totalPending").value(50.00))
                .andExpect(jsonPath("$.categoryBreakdown[0].category").value("ALIMENTACAO"));
    }
}

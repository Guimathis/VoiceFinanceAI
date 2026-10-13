package com.voicefinance.controller;

import com.voicefinance.domain.commands.ListExpensesCommand;
import com.voicefinance.domain.dto.*;
import com.voicefinance.domain.enums.Category;
import com.voicefinance.domain.enums.ExpenseStatus;
import com.voicefinance.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Despesas", description = "Gestão de despesas, consultas paginadas, resumo e ações diretas")
@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @Operation(summary = "Criar despesa manual", description = "Registra uma nova despesa diretamente com status CONFIRMED.")
    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(@RequestBody @Valid CreateExpenseRequest request) {
        ExpenseResponse response = expenseService.createManualExpense(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Obter despesa por ID", description = "Recupera os detalhes completos da despesa informada.")
    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> getExpenseById(@PathVariable UUID id) {
        ExpenseResponse response = expenseService.getExpenseById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar despesas com paginação e filtros", description = "Lista despesas aplicando filtros de período, categoria, status ou busca textual.")
    @GetMapping
    public ResponseEntity<PageResponse<ExpenseResponse>> listExpenses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) ExpenseStatus status,
            @RequestParam(required = false) String description,
            @PageableDefault(size = 10, sort = "occurredOn", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        ListExpensesCommand command = new ListExpensesCommand(
                startDate,
                endDate,
                category,
                status,
                description,
                pageable.getPageSize()
        );
        Page<ExpenseResponse> page = expenseService.findExpensesPaginated(command, pageable);
        return ResponseEntity.ok(PageResponse.from(page));
    }

    @Operation(summary = "Atualizar despesa", description = "Atualiza atributos de uma despesa que ainda esteja em estado editável.")
    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateExpenseRequest request
    ) {
        ExpenseResponse response = expenseService.updateExpense(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Confirmar despesa", description = "Confirma diretamente uma despesa pendente.")
    @PostMapping("/{id}/confirm")
    public ResponseEntity<ExpenseResponse> confirmExpense(@PathVariable UUID id) {
        ExpenseResponse response = expenseService.confirmExpenseDirect(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cancelar despesa", description = "Cancela diretamente uma despesa em aberto.")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ExpenseResponse> cancelExpense(@PathVariable UUID id) {
        ExpenseResponse response = expenseService.cancelExpenseDirect(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Excluir despesa", description = "Remove definitivamente uma despesa do banco de dados.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable UUID id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Resumo financeiro", description = "Retorna os totais de gastos, pendências e distribuição por categorias para o período informado.")
    @GetMapping("/summary")
    public ResponseEntity<ExpenseSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        ExpenseSummaryResponse summary = expenseService.getExpenseSummary(startDate, endDate);
        return ResponseEntity.ok(summary);
    }
}

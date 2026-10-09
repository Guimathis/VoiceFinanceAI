package com.voicefinance.controller;

import com.voicefinance.domain.dto.AudioChatResponse;
import com.voicefinance.service.audio.AudioExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Voz e Áudio", description = "Endpoints para envio e processamento de áudio para registro de despesas")
@RestController
@RequestMapping("/api/v1/voice")
@RequiredArgsConstructor
public class AudioController {

    private final AudioExpenseService audioExpenseService;

    @Operation(
            summary = "Processar áudio de despesa",
            description = "Recebe um arquivo de áudio, valida formatos e limites, armazena a captura, transcreve via IA e repassa a mensagem ao agente financeiro."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Áudio processado e transcrito com sucesso"),
            @ApiResponse(responseCode = "400", description = "Arquivo vazio, duração excedida ou áudio inaudível",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "413", description = "Arquivo maior que o limite configurado (10 MB)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "415", description = "Formato de arquivo ou assinatura de áudio não suportados",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno ao processar áudio",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(path = "/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AudioChatResponse> audio(
            @Parameter(description = "Arquivo de áudio (MP3, WAV, OGG, WebM ou M4A)", required = true)
            @RequestPart("file") MultipartFile file
    ) {
        AudioChatResponse response = audioExpenseService.processAudio(file);
        return ResponseEntity.ok(response);
    }
}

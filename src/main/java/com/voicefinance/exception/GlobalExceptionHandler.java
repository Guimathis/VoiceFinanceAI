package com.voicefinance.exception;

import com.google.genai.errors.ServerException;
import io.opencensus.trace.Span;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        log.warn("Falha de validação dos dados da requisição: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Um ou mais campos da requisição falharam na validação"
        );
        problem.setTitle("Validation Failed");
        problem.setProperty("timestamp", Instant.now());

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        problem.setProperty("errors", fieldErrors);

        return ResponseEntity.status(status).headers(headers).body(problem);
    }

    @ExceptionHandler(ExpenseNotFoundException.class)
    public ProblemDetail handleExpenseNotFoundException(ExpenseNotFoundException ex) {
        log.warn("Despesa não encontrada: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Expense Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ExpenseNotEditableException.class)
    public ProblemDetail handleExpenseNotEditableException(ExpenseNotEditableException ex) {
        log.warn("Despesa não editável: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Expense Not Editable");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(EmptyAudioException.class)
    public ProblemDetail handleEmptyAudioException(EmptyAudioException ex) {
        log.warn("Arquivo de áudio vazio: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Empty Audio");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "AUDIO_EMPTY");
        return problem;
    }

    @ExceptionHandler(AudioTooLargeException.class)
    public ProblemDetail handleAudioTooLargeException(AudioTooLargeException ex) {
        log.warn("Arquivo de áudio excedeu o tamanho máximo: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage());
        problem.setTitle("Payload Too Large");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "AUDIO_TOO_LARGE");
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            org.springframework.web.multipart.MaxUploadSizeExceededException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        log.warn("Tamanho máximo de upload excedido pelo Spring: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "O arquivo enviado excede o limite máximo permitido de 10 MB."
        );
        problem.setTitle("Payload Too Large");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "AUDIO_TOO_LARGE");
        return ResponseEntity.status(status).headers(headers).body(problem);
    }

    @ExceptionHandler(InvalidAudioFormatException.class)
    public ProblemDetail handleInvalidAudioFormatException(InvalidAudioFormatException ex) {
        log.warn("Formato de áudio inválido: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
        problem.setTitle("Unsupported Media Type");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "AUDIO_FORMAT_UNSUPPORTED");
        return problem;
    }

    @ExceptionHandler(AudioDurationExceededException.class)
    public ProblemDetail handleAudioDurationExceededException(AudioDurationExceededException ex) {
        log.warn("Duração do áudio excedida: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Audio Duration Exceeded");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "AUDIO_DURATION_EXCEEDED");
        return problem;
    }

    @ExceptionHandler(InaudibleAudioException.class)
    public ProblemDetail handleInaudibleAudioException(InaudibleAudioException ex) {
        log.warn("Áudio inaudível ou sem fala: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Transcription Inaudible");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "AUDIO_INAUDIBLE");
        return problem;
    }

    @ExceptionHandler(AudioStorageException.class)
    public ProblemDetail handleAudioStorageException(AudioStorageException ex) {
        log.error("Erro no armazenamento de áudio: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problem.setTitle("Audio Storage Error");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "AUDIO_STORAGE_ERROR");
        return problem;
    }

    @ExceptionHandler(ServerException.class)
    public ProblemDetail handleServerException(ServerException ex) {
        log.error("Erro no serviço genai inesperado", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado no serviço genai."
        );
        problem.setTitle("Internal Server Error");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUncaughtException(Exception ex) {
        log.error("Erro interno inesperado", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno inesperado."
        );
        problem.setTitle("Internal Server Error");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
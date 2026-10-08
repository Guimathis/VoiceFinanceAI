package com.voicefinance.controller;

import com.voicefinance.ai.client.ExpenseClient;
import com.voicefinance.service.TranscriptionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController(value = "/voice")
public class AudioController {

    private final ExpenseClient expenseClient;
    private final ChatClient chatClient;
    private final TranscriptionService transcriptionService;

    public AudioController(ExpenseClient expenseClient, @Qualifier("transcriptionClient") ChatClient chatClient, TranscriptionService transcriptionService) {
        this.expenseClient = expenseClient;
        this.chatClient = chatClient;
        this.transcriptionService = transcriptionService;
    }

    @PostMapping(path = "/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity.BodyBuilder audio(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok();
    }

//    @PostMapping(value = "/chat")
//    public String chat(@RequestParam String message) {
//        return chatService.chat(message);
//    }
//
//    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    Flux<String> chatFlux(@RequestParam String msg) {
//        return chatClient.prompt().user(msg).stream().content();
//    }
//
//
//    @PostMapping(path = "/transcrever", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    String transcrever(@RequestPart("file") MultipartFile file) {
//        return transcriptionService.transcrever(file.getResource(), file.getContentType());
//    }

}

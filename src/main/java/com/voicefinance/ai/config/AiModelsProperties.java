package com.voicefinance.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiModelsProperties(String transcriptionModel, String agentModel) {}

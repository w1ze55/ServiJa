package com.servija.servija.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    @PostConstruct
    public void logConfig() {
        log.info("GeminiService inicializado — model='{}', apiKey='{}'",
                model,
                apiKey != null && apiKey.length() > 8
                        ? apiKey.substring(0, 8) + "..."
                        : "(não configurada)");
    }

    public String generateText(String prompt) {
        log.debug("Chamando Gemini API com model='{}'", model);
        try {
            Client client = Client.builder().apiKey(apiKey).build();
            GenerateContentResponse response = client.models.generateContent(
                    model,
                    prompt,
                    null
            );
            String text = response.text();
            if (text == null || text.isBlank()) {
                throw new RuntimeException("Gemini retornou resposta vazia");
            }
            return text.trim();
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (msg.contains("429") || msg.contains("quota") || msg.contains("RESOURCE_EXHAUSTED")) {
                log.error("Gemini quota excedida (model='{}'): {}", model, msg);
                throw new RuntimeException("Quota da API Gemini excedida. Tente novamente em alguns instantes.", e);
            }
            log.error("Erro ao chamar Gemini API (model='{}'): {}", model, msg);
            throw new RuntimeException("Falha ao gerar texto com Gemini: " + msg, e);
        }
    }
}

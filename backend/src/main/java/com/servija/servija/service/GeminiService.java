package com.servija.servija.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    public String generateText(String prompt) {
        try {
            Client client = Client.builder().apiKey(apiKey).build();
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.0-flash",
                    prompt,
                    null
            );
            String text = response.text();
            if (text == null || text.isBlank()) {
                throw new RuntimeException("Gemini retornou resposta vazia");
            }
            return text.trim();
        } catch (Exception e) {
            log.error("Erro ao chamar Gemini API", e);
            throw new RuntimeException("Falha ao gerar texto com Gemini: " + e.getMessage(), e);
        }
    }
}

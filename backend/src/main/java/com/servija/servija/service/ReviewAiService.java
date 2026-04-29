package com.servija.servija.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servija.servija.dto.AiSummaryResponse;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.Review;
import com.servija.servija.model.ReviewSummary;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.ReviewRepository;
import com.servija.servija.repository.ReviewSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewAiService {

    private static final int MIN_REVIEWS_FOR_SUMMARY = 3;
    private static final int MAX_COMMENT_LENGTH = 480;
    private static final int CACHE_HOURS = 24;

    private final GeminiService geminiService;
    private final ReviewRepository reviewRepository;
    private final ReviewSummaryRepository reviewSummaryRepository;
    private final FreelancerRepository freelancerRepository;
    private final ObjectMapper objectMapper;

    public String refineBio(String bio) {
        StringBuilder sb = new StringBuilder();
        sb.append("Você é um redator profissional especializado em perfis de freelancers no Brasil.\n");
        sb.append("Refine a biografia abaixo escrita por um freelancer para o seu perfil profissional.\n");
        sb.append("Regras:\n");
        sb.append("- PRESERVE todas as informações, experiências e habilidades mencionadas pelo profissional\n");
        sb.append("- Corrija erros gramaticais e melhore a fluência em PT-BR\n");
        sb.append("- Torne o texto mais profissional, claro e persuasivo\n");
        sb.append("- Mantenha a voz em primeira pessoa\n");
        sb.append("- O texto refinado deve ter no máximo 1000 caracteres\n");
        sb.append("- Retorne APENAS o texto da biografia refinada, sem explicações, sem aspas\n\n");
        sb.append("Biografia original:\n").append(bio).append("\n");

        String response = geminiService.generateText(sb.toString());
        if (response.length() > 1000) {
            response = response.substring(0, 1000);
        }
        return response;
    }

    public String suggestComment(Integer rating, List<String> tags, String serviceName, String freelancerName, String existingComment) {
        String prompt = buildCommentPrompt(rating, tags, serviceName, freelancerName, existingComment);
        String response = geminiService.generateText(prompt);
        if (response.length() > MAX_COMMENT_LENGTH) {
            response = response.substring(0, MAX_COMMENT_LENGTH);
        }
        return response;
    }

    @Transactional
    public AiSummaryResponse getOrGenerateSummary(Long freelancerId) {
        Freelancer freelancer = freelancerRepository.findById(freelancerId)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer não encontrado"));

        List<Review> reviews = reviewRepository.findByReviewedIdOrderByCreatedAtDesc(freelancerId);
        int currentReviewCount = reviews.size();

        if (currentReviewCount < MIN_REVIEWS_FOR_SUMMARY) {
            return AiSummaryResponse.unavailable();
        }

        Optional<ReviewSummary> cached = reviewSummaryRepository.findByFreelancerId(freelancerId);
        if (cached.isPresent()) {
            ReviewSummary summary = cached.get();
            boolean notExpired = summary.getGeneratedAt().isAfter(LocalDateTime.now().minusHours(CACHE_HOURS));
            boolean sameCount = summary.getReviewCount() == currentReviewCount;
            if (notExpired && sameCount) {
                return parseStoredSummary(summary.getSummaryJson(), summary.getGeneratedAt());
            }
        }

        try {
            String prompt = buildSummaryPrompt(reviews);
            String geminiResponse = geminiService.generateText(prompt);
            String cleanJson = extractJson(geminiResponse);

            Map<String, Object> parsed = objectMapper.readValue(cleanJson, new TypeReference<>() {});

            @SuppressWarnings("unchecked")
            List<String> strengths = (List<String>) parsed.getOrDefault("strengths", List.of());
            @SuppressWarnings("unchecked")
            List<String> attentionPoints = (List<String>) parsed.getOrDefault("attentionPoints", List.of());
            String overall = (String) parsed.getOrDefault("overall", "");

            LocalDateTime now = LocalDateTime.now();
            AiSummaryResponse result = AiSummaryResponse.builder()
                    .available(true)
                    .strengths(strengths)
                    .attentionPoints(attentionPoints)
                    .overall(overall)
                    .generatedAt(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();

            String summaryJson = objectMapper.writeValueAsString(Map.of(
                    "strengths", strengths,
                    "attentionPoints", attentionPoints,
                    "overall", overall
            ));

            ReviewSummary toSave = cached.map(existing -> {
                existing.setSummaryJson(summaryJson);
                existing.setReviewCount(currentReviewCount);
                existing.setGeneratedAt(now);
                return existing;
            }).orElse(ReviewSummary.builder()
                    .freelancer(freelancer)
                    .summaryJson(summaryJson)
                    .reviewCount(currentReviewCount)
                    .generatedAt(now)
                    .build());

            reviewSummaryRepository.save(toSave);
            return result;

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao gerar resumo de IA para freelancer {}", freelancerId, e);
            return AiSummaryResponse.unavailable();
        }
    }

    private String buildCommentPrompt(Integer rating, List<String> tags, String serviceName, String freelancerName, String existingComment) {
        StringBuilder sb = new StringBuilder();
        sb.append("Você é um assistente que ajuda clientes a escrever avaliações honestas e construtivas em PT-BR.\n");

        boolean hasExisting = existingComment != null && !existingComment.isBlank();

        if (hasExisting) {
            sb.append("O cliente já começou a escrever a avaliação abaixo. Sua tarefa é REFINAR esse texto: ");
            sb.append("corrija erros, melhore a fluência e enriqueça o conteúdo, mas PRESERVE a opinião, o tom e as ideias originais do cliente.\n");
            sb.append("NÃO invente informações que não estejam no texto original.\n");
        } else {
            sb.append("Gere um comentário de avaliação de serviço com base nas informações abaixo.\n");
        }

        sb.append("O comentário deve ter entre 80 e 200 caracteres, ser natural e pessoal.\n");
        sb.append("Retorne APENAS o texto do comentário, sem aspas, sem explicações.\n\n");
        sb.append("Nota dada: ").append(rating).append(" de 5 estrelas\n");

        if (serviceName != null && !serviceName.isBlank()) {
            sb.append("Serviço: ").append(serviceName).append("\n");
        }
        if (freelancerName != null && !freelancerName.isBlank()) {
            sb.append("Nome do freelancer: ").append(freelancerName).append("\n");
        }
        if (tags != null && !tags.isEmpty()) {
            sb.append("Pontos destacados: ").append(String.join(", ", tags)).append("\n");
        }
        if (hasExisting) {
            sb.append("\nTexto original do cliente (refine este):\n").append(existingComment).append("\n");
        }

        return sb.toString();
    }

    private String buildSummaryPrompt(List<Review> reviews) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analise as avaliações abaixo de um freelancer e gere um resumo objetivo em PT-BR.\n");
        sb.append("Retorne APENAS um JSON válido (sem markdown, sem ```json) com o formato:\n");
        sb.append("{\"strengths\": [\"ponto 1\", \"ponto 2\"], \"attentionPoints\": [\"ponto 1\"], \"overall\": \"síntese curta\"}\n");
        sb.append("- overall: parágrafo curto de 1 resumindo o perfil\n\n");
        sb.append("Avaliações:\n");

        reviews.stream().limit(20).forEach(r -> {
            sb.append("- Nota: ").append(r.getRating()).append("/5");
            if (r.getTags() != null && !r.getTags().isBlank()) {
                sb.append(" | Tags: ").append(r.getTags());
            }
            if (r.getComment() != null && !r.getComment().isBlank()) {
                sb.append(" | Comentário: ").append(r.getComment());
            }
            sb.append("\n");
        });

        return sb.toString();
    }

    private String extractJson(String raw) {
        String trimmed = raw.strip();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).strip();
            }
        }
        return trimmed;
    }

    private AiSummaryResponse parseStoredSummary(String summaryJson, LocalDateTime generatedAt) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(summaryJson, new TypeReference<>() {});

            @SuppressWarnings("unchecked")
            List<String> strengths = (List<String>) parsed.getOrDefault("strengths", List.of());
            @SuppressWarnings("unchecked")
            List<String> attentionPoints = (List<String>) parsed.getOrDefault("attentionPoints", List.of());
            String overall = (String) parsed.getOrDefault("overall", "");

            return AiSummaryResponse.builder()
                    .available(true)
                    .strengths(strengths)
                    .attentionPoints(attentionPoints)
                    .overall(overall)
                    .generatedAt(generatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();
        } catch (Exception e) {
            log.error("Erro ao parsear resumo armazenado", e);
            return AiSummaryResponse.unavailable();
        }
    }
}

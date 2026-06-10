package com.servija.servija.controller;

import com.servija.servija.dto.AiCommentRequest;
import com.servija.servija.dto.AiCommentResponse;
import com.servija.servija.dto.AiSummaryResponse;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.exception.TooManyAttemptsException;
import com.servija.servija.service.RateLimiterService;
import com.servija.servija.service.ReviewAiService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews/ai")
@RequiredArgsConstructor
@Slf4j
public class ReviewAiController {

    private final ReviewAiService reviewAiService;
    private final RateLimiterService rateLimiter;

    @PostMapping("/suggest-comment")
    public ResponseEntity<AiCommentResponse> suggestComment(
            @RequestBody AiCommentRequest request,
            Authentication authentication) {
        try {
            String suggestion = reviewAiService.suggestComment(
                    request.getRating(),
                    request.getTags(),
                    request.getServiceName(),
                    request.getFreelancerName(),
                    request.getExistingComment()
            );
            return ResponseEntity.ok(new AiCommentResponse(suggestion));
        } catch (Exception e) {
            log.error("Erro ao gerar sugestão de comentário", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/summary/{freelancerId}")
    public ResponseEntity<AiSummaryResponse> getAiSummary(@PathVariable Long freelancerId,
            HttpServletRequest httpRequest) {
        // M10: endpoint público que pode disparar chamada paga ao Gemini — limita por IP
        // para evitar custo/DoS anônimo.
        String forwarded = httpRequest.getHeader("X-Forwarded-For");
        String ip = (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : httpRequest.getRemoteAddr();
        if (!rateLimiter.tryConsume("ai-summary:ip:" + ip, 30, 10 * 60 * 1000L)) {
            throw new TooManyAttemptsException("Muitas solicitações. Tente novamente em instantes.", 0, null);
        }
        try {
            AiSummaryResponse summary = reviewAiService.getOrGenerateSummary(freelancerId);
            return ResponseEntity.ok(summary);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erro ao obter resumo de IA para freelancer {}", freelancerId, e);
            return ResponseEntity.ok(AiSummaryResponse.unavailable());
        }
    }
}

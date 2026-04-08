package com.servija.servija.controller;

import com.servija.servija.dto.AiCommentRequest;
import com.servija.servija.dto.AiCommentResponse;
import com.servija.servija.dto.AiSummaryResponse;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.service.ReviewAiService;
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

    @PostMapping("/suggest-comment")
    public ResponseEntity<AiCommentResponse> suggestComment(
            @RequestBody AiCommentRequest request,
            Authentication authentication) {
        try {
            String suggestion = reviewAiService.suggestComment(
                    request.getRating(),
                    request.getTags(),
                    request.getServiceName(),
                    request.getFreelancerName()
            );
            return ResponseEntity.ok(new AiCommentResponse(suggestion));
        } catch (Exception e) {
            log.error("Erro ao gerar sugestão de comentário", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/summary/{freelancerId}")
    public ResponseEntity<AiSummaryResponse> getAiSummary(@PathVariable Long freelancerId) {
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

package com.servija.servija.controller;

import com.servija.servija.dto.RefineBioRequest;
import com.servija.servija.dto.RefineBioResponse;
import com.servija.servija.service.ReviewAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/freelancer/ai")
@RequiredArgsConstructor
@Slf4j
public class FreelancerAiController {

    private final ReviewAiService reviewAiService;

    @PostMapping("/refine-bio")
    public ResponseEntity<RefineBioResponse> refineBio(@RequestBody RefineBioRequest request) {
        try {
            if (request.getBio() == null || request.getBio().isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            String refined = reviewAiService.refineBio(request.getBio());
            return ResponseEntity.ok(new RefineBioResponse(refined));
        } catch (Exception e) {
            log.error("Erro ao refinar biografia com IA", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

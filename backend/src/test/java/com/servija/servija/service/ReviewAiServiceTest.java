package com.servija.servija.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servija.servija.dto.AiSummaryResponse;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.Review;
import com.servija.servija.model.ReviewSummary;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.ReviewRepository;
import com.servija.servija.repository.ReviewSummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewAiServiceTest {

    @Mock
    private GeminiService geminiService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewSummaryRepository reviewSummaryRepository;

    @Mock
    private FreelancerRepository freelancerRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ReviewAiService reviewAiService;

    private Freelancer freelancer;

    @BeforeEach
    void setUp() {
        freelancer = new Freelancer();
        freelancer.setId(1L);
    }

    // --- suggestComment ---

    @Test
    @DisplayName("suggestComment retorna texto gerado pelo Gemini")
    void suggestComment_returnsGeminiText() {
        when(geminiService.generateText(anyString())).thenReturn("Ótimo profissional, muito pontual!");

        String result = reviewAiService.suggestComment(5, List.of("Pontualidade"), "Limpeza", "João");

        assertThat(result).isEqualTo("Ótimo profissional, muito pontual!");
        verify(geminiService).generateText(anyString());
    }

    @Test
    @DisplayName("suggestComment trunca resposta maior que 480 chars")
    void suggestComment_truncatesLongResponse() {
        String longText = "a".repeat(600);
        when(geminiService.generateText(anyString())).thenReturn(longText);

        String result = reviewAiService.suggestComment(4, Collections.emptyList(), null, null);

        assertThat(result).hasSize(480);
    }

    @Test
    @DisplayName("suggestComment propaga RuntimeException quando Gemini falha")
    void suggestComment_propagatesExceptionOnGeminiFailure() {
        when(geminiService.generateText(anyString())).thenThrow(new RuntimeException("Gemini error"));

        assertThatThrownBy(() ->
                reviewAiService.suggestComment(3, List.of("Comunicação"), "Pintura", "Maria"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Gemini error");
    }

    // --- getOrGenerateSummary ---

    @Test
    @DisplayName("getOrGenerateSummary retorna available=false quando freelancer tem menos de 3 reviews")
    void getOrGenerateSummary_unavailableWhenLessThan3Reviews() {
        when(freelancerRepository.findById(1L)).thenReturn(Optional.of(freelancer));
        when(reviewRepository.findByReviewedIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(makeReview(5)));

        AiSummaryResponse result = reviewAiService.getOrGenerateSummary(1L);

        assertThat(result.isAvailable()).isFalse();
        verifyNoInteractions(geminiService);
    }

    @Test
    @DisplayName("getOrGenerateSummary retorna cache quando válido")
    void getOrGenerateSummary_returnsCacheWhenValid() throws Exception {
        List<Review> reviews = List.of(makeReview(5), makeReview(4), makeReview(5));
        when(freelancerRepository.findById(1L)).thenReturn(Optional.of(freelancer));
        when(reviewRepository.findByReviewedIdOrderByCreatedAtDesc(1L)).thenReturn(reviews);

        String json = "{\"strengths\":[\"Pontualidade\"],\"attentionPoints\":[],\"overall\":\"Excelente\"}";
        ReviewSummary cached = ReviewSummary.builder()
                .freelancer(freelancer)
                .summaryJson(json)
                .reviewCount(3)
                .generatedAt(LocalDateTime.now().minusHours(1))
                .build();
        when(reviewSummaryRepository.findByFreelancerId(1L)).thenReturn(Optional.of(cached));

        AiSummaryResponse result = reviewAiService.getOrGenerateSummary(1L);

        assertThat(result.isAvailable()).isTrue();
        assertThat(result.getStrengths()).containsExactly("Pontualidade");
        assertThat(result.getOverall()).isEqualTo("Excelente");
        verifyNoInteractions(geminiService);
    }

    @Test
    @DisplayName("getOrGenerateSummary gera novo resumo quando cache expirado")
    void getOrGenerateSummary_generatesNewWhenCacheExpired() throws Exception {
        List<Review> reviews = List.of(makeReview(5), makeReview(4), makeReview(5));
        when(freelancerRepository.findById(1L)).thenReturn(Optional.of(freelancer));
        when(reviewRepository.findByReviewedIdOrderByCreatedAtDesc(1L)).thenReturn(reviews);

        ReviewSummary expiredCache = ReviewSummary.builder()
                .freelancer(freelancer)
                .summaryJson("{\"strengths\":[],\"attentionPoints\":[],\"overall\":\"old\"}")
                .reviewCount(3)
                .generatedAt(LocalDateTime.now().minusDays(2))
                .build();
        when(reviewSummaryRepository.findByFreelancerId(1L)).thenReturn(Optional.of(expiredCache));

        String geminiJson = "{\"strengths\":[\"Qualidade\",\"Pontualidade\"],\"attentionPoints\":[],\"overall\":\"Profissional excelente.\"}";
        when(geminiService.generateText(anyString())).thenReturn(geminiJson);
        when(reviewSummaryRepository.save(any())).thenReturn(expiredCache);

        AiSummaryResponse result = reviewAiService.getOrGenerateSummary(1L);

        assertThat(result.isAvailable()).isTrue();
        assertThat(result.getStrengths()).contains("Qualidade", "Pontualidade");
        verify(geminiService).generateText(anyString());
        verify(reviewSummaryRepository).save(any());
    }

    @Test
    @DisplayName("getOrGenerateSummary retorna available=false quando Gemini falha")
    void getOrGenerateSummary_unavailableWhenGeminiFails() {
        List<Review> reviews = List.of(makeReview(5), makeReview(4), makeReview(5));
        when(freelancerRepository.findById(1L)).thenReturn(Optional.of(freelancer));
        when(reviewRepository.findByReviewedIdOrderByCreatedAtDesc(1L)).thenReturn(reviews);
        when(reviewSummaryRepository.findByFreelancerId(1L)).thenReturn(Optional.empty());
        when(geminiService.generateText(anyString())).thenThrow(new RuntimeException("API timeout"));

        AiSummaryResponse result = reviewAiService.getOrGenerateSummary(1L);

        assertThat(result.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("getOrGenerateSummary lança ResourceNotFoundException quando freelancer não existe")
    void getOrGenerateSummary_throwsNotFoundWhenFreelancerMissing() {
        when(freelancerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewAiService.getOrGenerateSummary(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Review makeReview(int rating) {
        Review r = new Review();
        r.setRating(rating);
        return r;
    }
}

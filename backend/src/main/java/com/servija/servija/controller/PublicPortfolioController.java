package com.servija.servija.controller;

import com.servija.servija.dto.PortfolioProjectResponse;
import com.servija.servija.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
@Tag(name = "Public Portfolio", description = "Endpoints públicos para consulta de portfólio de freelancers")
public class PublicPortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping("/freelancer/{freelancerId}")
    @Operation(
        summary = "Listar projetos de um freelancer específico",
        description = "Retorna todos os projetos do portfólio de um freelancer por ID (não requer autenticação)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de projetos retornada com sucesso",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = PortfolioProjectResponse.class)))
        ),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<PortfolioProjectResponse>> getFreelancerProjects(@PathVariable Long freelancerId) {
        try {
            List<PortfolioProjectResponse> projects = portfolioService.getPublicProjects(freelancerId);
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

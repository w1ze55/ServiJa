package com.servija.servija.controller;

import com.servija.servija.dto.MessageResponse;
import com.servija.servija.dto.PortfolioProjectRequest;
import com.servija.servija.dto.PortfolioProjectResponse;
import com.servija.servija.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/freelancer/portfolio")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Portfolio", description = "Endpoints para gerenciamento do portfólio de projetos do freelancer")
@SecurityRequirement(name = "bearerAuth")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping
    @Operation(
        summary = "Listar todos os projetos",
        description = "Retorna todos os projetos do portfólio do freelancer autenticado"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de projetos retornada com sucesso",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = PortfolioProjectResponse.class)))
        ),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<PortfolioProjectResponse>> getProjects() {
        try {
            List<PortfolioProjectResponse> projects = portfolioService.getProjects();
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Obter projeto específico",
        description = "Retorna os detalhes de um projeto específico do portfólio"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Projeto retornado com sucesso",
            content = @Content(schema = @Schema(implementation = PortfolioProjectResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Projeto não encontrado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<PortfolioProjectResponse> getProject(@PathVariable Long id) {
        try {
            PortfolioProjectResponse project = portfolioService.getProject(id);
            return ResponseEntity.ok(project);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    @Operation(
        summary = "Criar novo projeto",
        description = "Adiciona um novo projeto ao portfólio do freelancer"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Projeto criado com sucesso",
            content = @Content(schema = @Schema(implementation = PortfolioProjectResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<PortfolioProjectResponse> createProject(@Valid @RequestBody PortfolioProjectRequest request) {
        try {
            PortfolioProjectResponse project = portfolioService.createProject(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(project);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Atualizar projeto",
        description = "Atualiza um projeto existente do portfólio"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Projeto atualizado com sucesso",
            content = @Content(schema = @Schema(implementation = PortfolioProjectResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Projeto não encontrado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<PortfolioProjectResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody PortfolioProjectRequest request
    ) {
        try {
            log.info("Recebida requisição para atualizar projeto {}", id);
            PortfolioProjectResponse project = portfolioService.updateProject(id, request);
            log.info("Retornando projeto atualizado: {}", project.getId());
            return ResponseEntity.ok(project);
        } catch (RuntimeException e) {
            log.error("RuntimeException ao atualizar projeto {}: {}", id, e.getMessage(), e);
            if (e.getMessage() != null && e.getMessage().contains("não encontrado")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("Exception ao atualizar projeto {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Excluir projeto",
        description = "Remove um projeto do portfólio do freelancer"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Projeto excluído com sucesso",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "404", description = "Projeto não encontrado"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<MessageResponse> deleteProject(@PathVariable Long id) {
        try {
            portfolioService.deleteProject(id);
            return ResponseEntity.ok(new MessageResponse("Projeto excluído com sucesso"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

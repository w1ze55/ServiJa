package com.servija.servija.controller;

import com.servija.servija.dto.FreelancerProfileRequest;
import com.servija.servija.dto.FreelancerProfileResponse;
import com.servija.servija.dto.MessageResponse;
import com.servija.servija.dto.NearbyFreelancerResponse;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.service.FreelancerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/freelancer")
@RequiredArgsConstructor
@Tag(name = "Freelancers", description = "Endpoints para gerenciamento de perfis de freelancers")
public class FreelancerController {

    private final FreelancerService freelancerService;

    @GetMapping("/{id}/profile")
    @Operation(
        summary = "Obter perfil público do freelancer",
        description = "Retorna o perfil público de um freelancer por ID (não requer autenticação)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Perfil retornado com sucesso",
            content = @Content(schema = @Schema(implementation = FreelancerProfileResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Freelancer não encontrado"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Erro interno do servidor"
        )
    })
    public ResponseEntity<FreelancerProfileResponse> getFreelancerPublicProfile(@PathVariable Long id) {
        try {
            FreelancerProfileResponse profile = freelancerService.getVisiblePublicProfile(id);
            return ResponseEntity.ok(profile);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/profile")
    @Operation(
        summary = "Obter perfil do freelancer",
        description = "Retorna o perfil do freelancer autenticado"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Perfil retornado com sucesso",
            content = @Content(schema = @Schema(implementation = FreelancerProfileResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Erro interno do servidor"
        )
    })
    public ResponseEntity<FreelancerProfileResponse> getProfile() {
        try {
            FreelancerProfileResponse profile = freelancerService.getProfile();
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/profile")
    @Operation(
        summary = "Atualizar perfil do freelancer",
        description = "Atualiza os dados do perfil do freelancer autenticado"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Perfil atualizado com sucesso",
            content = @Content(schema = @Schema(implementation = FreelancerProfileResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado"
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Serviço externo de validação indisponível"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Erro interno do servidor"
        )
    })
    public ResponseEntity<FreelancerProfileResponse> updateProfile(@RequestBody FreelancerProfileRequest request) {
        FreelancerProfileResponse profile = freelancerService.updateProfile(request);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/nearby")
    @Operation(
        summary = "Buscar freelancers próximos",
        description = "Busca freelancers próximos de uma localização específica, com raio e categoria opcionais"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de freelancers próximos retornada com sucesso",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = NearbyFreelancerResponse.class)))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Erro interno do servidor"
        )
    })
    public ResponseEntity<List<NearbyFreelancerResponse>> getNearbyFreelancers(
            @Parameter(description = "Latitude da localização de busca", required = true)
            @RequestParam BigDecimal latitude,
            
            @Parameter(description = "Longitude da localização de busca", required = true)
            @RequestParam BigDecimal longitude,
            
            @Parameter(description = "Raio de busca em quilômetros", example = "10")
            @RequestParam(defaultValue = "10") Double radius,
            
            @Parameter(description = "Categoria de serviço para filtrar (opcional)")
            @RequestParam(required = false) String category,

            @RequestParam(required = false, name = "q") String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) Long subcategoryId,
            @RequestParam(required = false) String subcategorySlug,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state
    ) {
        try {
            List<NearbyFreelancerResponse> nearbyFreelancers = freelancerService.findNearbyFreelancers(
                    latitude, longitude, radius, category, query, categoryId, categorySlug,
                    subcategoryId, subcategorySlug, minRating, available, city, state
            );
            return ResponseEntity.ok(nearbyFreelancers);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/location")
    @Operation(
        summary = "Atualizar localização do freelancer",
        description = "Atualiza a localização geográfica do freelancer autenticado"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Localização atualizada com sucesso",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Erro interno do servidor"
        )
    })
    public ResponseEntity<MessageResponse> updateLocation(
            @Parameter(description = "Latitude da nova localização", required = true)
            @RequestParam BigDecimal latitude,
            
            @Parameter(description = "Longitude da nova localização", required = true)
            @RequestParam BigDecimal longitude
    ) {
        try {
            freelancerService.updateLocation(latitude, longitude);
            return ResponseEntity.ok(new MessageResponse("Localização atualizada com sucesso"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}


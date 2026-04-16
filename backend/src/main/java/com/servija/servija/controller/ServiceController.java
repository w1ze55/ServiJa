package com.servija.servija.controller;

import com.servija.servija.dto.ServiceRequest;
import com.servija.servija.dto.ServiceResponse;
import com.servija.servija.service.ServiceService;
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

import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
@Tag(name = "Serviços", description = "Endpoints para gerenciamento de serviços oferecidos pelos freelancers")
public class ServiceController {

    private final ServiceService serviceService;

    @GetMapping("/my")
    @Operation(summary = "Listar meus serviços", description = "Retorna todos os serviços do freelancer autenticado")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de serviços retornada com sucesso", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ServiceResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Erro na requisição"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<List<ServiceResponse>> getMyServices() {
        try {
            List<ServiceResponse> services = serviceService.getMyServices();
            return ResponseEntity.ok(services);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Obter estatísticas dos serviços", description = "Retorna estatísticas dos serviços do freelancer autenticado")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estatísticas retornadas com sucesso", content = @Content(schema = @Schema(implementation = ServiceService.ServiceStats.class))),
            @ApiResponse(responseCode = "400", description = "Erro na requisição"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<ServiceService.ServiceStats> getServiceStats() {
        try {
            ServiceService.ServiceStats stats = serviceService.getServiceStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter serviço por ID", description = "Retorna os detalhes de um serviço específico")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Serviço encontrado", content = @Content(schema = @Schema(implementation = ServiceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<ServiceResponse> getServiceById(
            @Parameter(description = "ID do serviço", required = true) @PathVariable Long id) {
        try {
            ServiceResponse service = serviceService.getServiceById(id);
            return ResponseEntity.ok(service);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "Criar novo serviço", description = "Cria um novo serviço para o freelancer autenticado")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Serviço criado com sucesso", content = @Content(schema = @Schema(implementation = ServiceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<ServiceResponse> createService(@RequestBody ServiceRequest request) {
        try {
            ServiceResponse service = serviceService.createService(request);
            return ResponseEntity.ok(service);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar serviço", description = "Atualiza um serviço existente do freelancer autenticado")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Serviço atualizado com sucesso", content = @Content(schema = @Schema(implementation = ServiceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado")
    })
    public ResponseEntity<ServiceResponse> updateService(
            @Parameter(description = "ID do serviço", required = true) @PathVariable Long id,
            @RequestBody ServiceRequest request) {
        try {
            ServiceResponse service = serviceService.updateService(id, request);
            return ResponseEntity.ok(service);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir serviço", description = "Exclui um serviço do freelancer autenticado")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Serviço excluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro na requisição"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado")
    })
    public ResponseEntity<Void> deleteService(
            @Parameter(description = "ID do serviço", required = true) @PathVariable Long id) {
        try {
            serviceService.deleteService(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Alternar status do serviço", description = "Ativa ou desativa um serviço do freelancer autenticado")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status alterado com sucesso", content = @Content(schema = @Schema(implementation = ServiceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erro na requisição"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado")
    })
    public ResponseEntity<ServiceResponse> toggleServiceStatus(
            @Parameter(description = "ID do serviço", required = true) @PathVariable Long id) {
        try {
            ServiceResponse service = serviceService.toggleServiceStatus(id);
            return ResponseEntity.ok(service);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== ENDPOINTS PÚBLICOS ====================

    @GetMapping("/public/all")
    @Operation(summary = "Listar todos os serviços ativos (público)", 
                description = "Retorna todos os serviços ativos disponíveis na plataforma")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de serviços retornada com sucesso", 
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ServiceResponse.class))))
    })
    public ResponseEntity<List<ServiceResponse>> getAllActiveServices(
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) String subcategorySlug,
            @RequestParam(required = false, name = "q") String query) {
        try {
            List<ServiceResponse> services = serviceService.getActiveServices(categorySlug, subcategorySlug, query);
            return ResponseEntity.ok(services);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/public/category/{category}")
    @Operation(summary = "Buscar serviços por categoria (público)", description = "Retorna serviços públicos filtrados por categoria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de serviços retornada com sucesso", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ServiceResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Erro na requisição")
    })
    public ResponseEntity<List<ServiceResponse>> getServicesByCategory(
            @Parameter(description = "Categoria do serviço", required = true) @PathVariable String category) {
        try {
            List<ServiceResponse> services = serviceService.getServicesByCategory(category);
            return ResponseEntity.ok(services);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/public/{id}")
    @Operation(summary = "Obter serviço por ID (público)", description = "Retorna os detalhes de um serviço ativo específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Serviço encontrado", content = @Content(schema = @Schema(implementation = ServiceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado ou inativo")
    })
    public ResponseEntity<ServiceResponse> getPublicServiceById(
            @Parameter(description = "ID do serviço", required = true) @PathVariable Long id) {
        try {
            ServiceResponse service = serviceService.getPublicServiceByIdWithPricing(id);
            return ResponseEntity.ok(service);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/public/freelancer/{id}")
    @Operation(summary = "Listar todos os serviços ativos de um freelancer (público)", description = "Retorna todos os serviços ativos de um freelancer específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de serviços retornada com sucesso", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ServiceResponse.class))))
    })
    public ResponseEntity<List<ServiceResponse>> getServicesByFreelancer(
            @Parameter(description = "ID do freelancer", required = true) @PathVariable Long id) {
        try {
            List<ServiceResponse> services = serviceService.getPublicServicesByFreelancer(id);
            return ResponseEntity.ok(services);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/public/search")
    @Operation(summary = "Buscar serviços por texto (público)", description = "Realiza uma busca textual nos serviços públicos disponíveis")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de serviços retornada com sucesso", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ServiceResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Erro na requisição")
    })
    public ResponseEntity<List<ServiceResponse>> searchServices(
            @Parameter(description = "Termo de busca", required = true) @RequestParam String q) {
        try {
            List<ServiceResponse> services = serviceService.searchPublicServices(q);
            return ResponseEntity.ok(services);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

package com.servija.servija.controller;

import com.servija.servija.dto.ClientHomeResponse;
import com.servija.servija.service.ClientHomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client-home")
@RequiredArgsConstructor
@Tag(name = "Home do Cliente", description = "Endpoint agregado da página inicial do contratante")
@SecurityRequirement(name = "bearerAuth")
public class ClientHomeController {

    private final ClientHomeService clientHomeService;

    @GetMapping
    @Operation(
            summary = "Obter home do cliente",
            description = "Retorna os dados agregados da página inicial do contratante autenticado")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Home carregada com sucesso",
                    content = @Content(schema = @Schema(implementation = ClientHomeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Usuário inválido para a operação"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<ClientHomeResponse> getHome() {
        return ResponseEntity.ok(clientHomeService.getHome());
    }
}

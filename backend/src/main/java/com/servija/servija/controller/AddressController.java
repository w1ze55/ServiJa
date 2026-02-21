package com.servija.servija.controller;

import com.servija.servija.dto.AddressRequest;
import com.servija.servija.dto.AddressResponse;
import com.servija.servija.dto.MessageResponse;
import com.servija.servija.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Tag(name = "Endereços", description = "Endpoints para gerenciamento de endereços do usuário")
@SecurityRequirement(name = "bearerAuth")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @Operation(
        summary = "Listar endereços do usuário",
        description = "Retorna todos os endereços cadastrados pelo usuário autenticado"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de endereços retornada com sucesso",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AddressResponse.class)))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado"
        )
    })
    public ResponseEntity<List<AddressResponse>> getUserAddresses() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        List<AddressResponse> addresses = addressService.getUserAddresses(email);
        return ResponseEntity.ok(addresses);
    }

    @PostMapping
    @Operation(
        summary = "Criar novo endereço",
        description = "Cria um novo endereço para o usuário autenticado"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Endereço criado com sucesso",
            content = @Content(schema = @Schema(implementation = AddressResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado"
        )
    })
    public ResponseEntity<AddressResponse> createAddress(
            @Valid @RequestBody AddressRequest request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        AddressResponse address = addressService.createAddress(email, request);
        return ResponseEntity.ok(address);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Atualizar endereço",
        description = "Atualiza um endereço existente do usuário autenticado"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Endereço atualizado com sucesso",
            content = @Content(schema = @Schema(implementation = AddressResponse.class))
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
            responseCode = "404",
            description = "Endereço não encontrado"
        )
    })
    public ResponseEntity<AddressResponse> updateAddress(
            @Parameter(description = "ID do endereço", required = true)
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        AddressResponse address = addressService.updateAddress(email, id, request);
        return ResponseEntity.ok(address);
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Excluir endereço",
        description = "Exclui um endereço do usuário autenticado"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Endereço excluído com sucesso",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Endereço não encontrado"
        )
    })
    public ResponseEntity<MessageResponse> deleteAddress(
            @Parameter(description = "ID do endereço", required = true)
            @PathVariable Long id
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        addressService.deleteAddress(email, id);
        return ResponseEntity.ok(new MessageResponse("Endereço removido com sucesso"));
    }

    @PutMapping("/{id}/default")
    @Operation(
        summary = "Definir endereço padrão",
        description = "Define um endereço como padrão para o usuário autenticado"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Endereço padrão definido com sucesso",
            content = @Content(schema = @Schema(implementation = AddressResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Endereço não encontrado"
        )
    })
    public ResponseEntity<AddressResponse> setDefaultAddress(
            @Parameter(description = "ID do endereço", required = true)
            @PathVariable Long id
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        AddressResponse address = addressService.setDefaultAddress(email, id);
        return ResponseEntity.ok(address);
    }
}

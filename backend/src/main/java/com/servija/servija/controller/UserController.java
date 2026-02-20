package com.servija.servija.controller;

import com.servija.servija.dto.AuthResponse;
import com.servija.servija.dto.ChangePasswordRequest;
import com.servija.servija.dto.DeleteAccountRequest;
import com.servija.servija.dto.MessageResponse;
import com.servija.servija.dto.ProfileResponse;
import com.servija.servija.dto.UpdateAvatarRequest;
import com.servija.servija.dto.UpdateUserRequest;
import com.servija.servija.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Endpoints para gerenciamento de perfil do usuário autenticado")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(
        summary = "Obter usuário atual",
        description = "Retorna os dados do usuário autenticado"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Dados do usuário retornados com sucesso",
            content = @Content(schema = @Schema(implementation = AuthResponse.UserDto.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado"
        )
    })
    public ResponseEntity<AuthResponse.UserDto> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        AuthResponse.UserDto user = userService.getCurrentUser(email);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/me/profile")
    @Operation(
        summary = "Obter perfil completo",
        description = "Retorna o perfil completo do usuário autenticado incluindo dados específicos (freelancer ou cliente)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Perfil retornado com sucesso",
            content = @Content(schema = @Schema(implementation = ProfileResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado"
        )
    })
    public ResponseEntity<ProfileResponse> getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        ProfileResponse profile = userService.getCurrentUserProfile(email);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    @Operation(
        summary = "Atualizar dados do usuário",
        description = "Atualiza os dados básicos do usuário autenticado"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Dados atualizados com sucesso",
            content = @Content(schema = @Schema(implementation = AuthResponse.UserDto.class))
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
    public ResponseEntity<AuthResponse.UserDto> updateUser(
            @Valid @RequestBody UpdateUserRequest request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        AuthResponse.UserDto updatedUser = userService.updateUser(email, request);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/me/password")
    @Operation(
        summary = "Alterar senha",
        description = "Altera a senha do usuário autenticado"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Senha alterada com sucesso",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Senha atual incorreta ou nova senha inválida"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado"
        )
    })
    public ResponseEntity<MessageResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        userService.changePassword(email, request);
        return ResponseEntity.ok(new MessageResponse("Senha alterada com sucesso"));
    }

    @PutMapping("/me/avatar")
    @Operation(
        summary = "Atualizar avatar",
        description = "Atualiza a URL do avatar do usuário autenticado"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Avatar atualizado com sucesso",
            content = @Content(schema = @Schema(implementation = AuthResponse.UserDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "URL inválida"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado"
        )
    })
    public ResponseEntity<AuthResponse.UserDto> updateAvatar(
            @Valid @RequestBody UpdateAvatarRequest request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        AuthResponse.UserDto updatedUser = userService.updateAvatar(email, request);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/me")
    @Operation(
        summary = "Excluir conta",
        description = "Exclui permanentemente a conta do usuário autenticado"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Conta excluída com sucesso",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Senha incorreta"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado"
        )
    })
    public ResponseEntity<MessageResponse> deleteAccount(
            @Valid @RequestBody DeleteAccountRequest request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        userService.deleteAccount(email, request);
        return ResponseEntity.ok(new MessageResponse("Conta excluída com sucesso"));
    }
}


package com.servija.servija.controller;

import com.servija.servija.dto.*;
import com.servija.servija.repository.UserRepository;
import com.servija.servija.service.AuthService;
import com.servija.servija.service.EmailService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints para autenticação e gerenciamento de usuários")
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    @Operation(summary = "Registrar novo usuário", description = "Cria uma nova conta de usuário no sistema. Após o registro, um código de verificação é enviado para o e-mail fornecido.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário registrado com sucesso", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou e-mail já cadastrado", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/check-cpf")
    @Operation(summary = "Verificar disponibilidade de CPF", description = "Verifica se um CPF já está cadastrado no sistema. Endpoint público para validação em tempo real.")
    public ResponseEntity<MessageResponse> checkCpf(@RequestParam String cpf) {
        String cleanCpf = cpf.replaceAll("\\D", "");
        boolean exists = userRepository.existsByCpf(cleanCpf);
        if (exists) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new MessageResponse("CPF já cadastrado"));
        }
        return ResponseEntity.ok(new MessageResponse("CPF disponível"));
    }

    @PostMapping("/login")
    @Operation(summary = "Fazer login", description = "Autentica um usuário e retorna um token JWT para acesso às rotas protegidas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verificar e-mail", description = "Verifica o e-mail do usuário usando o código enviado durante o registro e retorna um token JWT para autenticação automática")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "E-mail verificado com sucesso", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Código inválido ou expirado", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        AuthResponse response = authService.verifyEmail(request.getEmail(), request.getCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Reenviar código de verificação", description = "Reenvia o código de verificação de e-mail para o usuário")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Código reenviado com sucesso", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<MessageResponse> resendVerification(@RequestParam String email) {
        authService.resendVerificationCode(email);
        return ResponseEntity.ok(new MessageResponse("Código de verificação reenviado"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar recuperação de senha", description = "Inicia o processo de recuperação de senha enviando um código para o e-mail do usuário")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Código de recuperação enviado", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        // Sempre retorna sucesso por segurança (não revelamos se o email existe)
        return ResponseEntity
                .ok(new MessageResponse("Se o e-mail estiver cadastrado, você receberá um código de recuperação"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Redefinir senha", description = "Redefine a senha do usuário usando o código de recuperação enviado por e-mail")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Senha redefinida com sucesso", content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Código inválido ou senhas não coincidem", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(
                request.getEmail(),
                request.getCode(),
                request.getNewPassword(),
                request.getConfirmPassword());
        return ResponseEntity.ok(new MessageResponse("Senha redefinida com sucesso"));
    }

    @PostMapping("/resend-reset-code")
    @Operation(summary = "Reenviar código de recuperação", description = "Reenvia o código de recuperação de senha para o e-mail do usuário")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Código reenviado com sucesso", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<MessageResponse> resendResetCode(@RequestParam String email) {
        authService.forgotPassword(email);
        return ResponseEntity.ok(new MessageResponse("Código de recuperação reenviado"));
    }

    @PostMapping("/test-email")
    @Operation(summary = "Testar envio de e-mail", description = "Endpoint de teste para verificar a configuração de envio de e-mails")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "E-mail de teste enviado", content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro ao enviar e-mail", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<MessageResponse> testEmail(@RequestParam String email) {
        try {
            emailService.sendVerificationEmail(email, "Usuário Teste", "TEST123");
            return ResponseEntity
                    .ok(new MessageResponse("Email de teste enviado! Verifique o console/logs para detalhes."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Erro ao enviar email: " + e.getMessage()));
        }
    }
}

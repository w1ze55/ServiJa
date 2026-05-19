package com.servija.servija.controller;

import com.servija.servija.dto.AuthResponse;
import com.servija.servija.dto.AddressRequest;
import com.servija.servija.dto.ForgotPasswordRequest;
import com.servija.servija.dto.LoginRequest;
import com.servija.servija.dto.MessageResponse;
import com.servija.servija.dto.RegistrationValidationRequest;
import com.servija.servija.dto.RegisterRequest;
import com.servija.servija.dto.ResetPasswordRequest;
import com.servija.servija.dto.VerifyEmailRequest;
import com.servija.servija.dto.VerifyResetPasswordCodeRequest;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacao", description = "Endpoints para autenticacao e gerenciamento de usuarios")
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    @PostMapping("/register")
    @Operation(summary = "Registrar novo usuario", description = "Cria uma nova conta de usuario no sistema. Apos o registro, um codigo de verificacao e enviado para o e-mail fornecido.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario registrado com sucesso", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados invalidos ou e-mail ja cadastrado", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/validate-registration")
    @Operation(summary = "Validar dados de cadastro", description = "Valida os dados do cadastro antes de prosseguir para a confirmacao da conta.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dados validados com sucesso", content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados invalidos", content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "409", description = "E-mail ou CPF ja cadastrado", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<MessageResponse> validateRegistration(@Valid @RequestBody RegistrationValidationRequest request) {
        authService.validateRegistrationData(request);
        return ResponseEntity.ok(new MessageResponse("Dados validados com sucesso"));
    }

    @PostMapping("/validate-address")
    @Operation(summary = "Validar endereço do cadastro", description = "Valida os dados do endereço informados na etapa de cadastro antes da confirmação.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Endereço validado com sucesso", content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Endereço inválido", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<MessageResponse> validateAddress(@Valid @RequestBody AddressRequest request) {
        authService.validateAddressData(request);
        return ResponseEntity.ok(new MessageResponse("Endereço validado com sucesso"));
    }

    @PostMapping("/login")
    @Operation(summary = "Fazer login", description = "Autentica um usuario e retorna um token JWT para acesso as rotas protegidas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais invalidas", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/login")
    @Operation(summary = "Login do painel administrativo", description = "Autentica apenas usuarios com perfil ADMIN. Demais tipos recebem erro de credenciais.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais invalidas ou usuario nao administrador", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<AuthResponse> loginAdmin(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.loginAdmin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verificar e-mail", description = "Verifica o e-mail do usuario usando o codigo enviado durante o registro e retorna um token JWT para autenticacao automatica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "E-mail verificado com sucesso", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Codigo invalido ou expirado", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        AuthResponse response = authService.verifyEmail(request.getEmail(), request.getCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Reenviar codigo de verificacao", description = "Reenvia o codigo de verificacao de e-mail para o usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Codigo reenviado com sucesso", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<MessageResponse> resendVerification(@RequestParam String email) {
        authService.resendVerificationCode(email);
        return ResponseEntity.ok(new MessageResponse("Codigo de verificacao reenviado"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar recuperacao de senha", description = "Inicia o processo de recuperacao de senha enviando um codigo para o e-mail do usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Codigo de recuperacao enviado", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(new MessageResponse("Se o e-mail estiver cadastrado, voce recebera um codigo de recuperacao"));
    }

    @PostMapping("/verify-reset-code")
    @Operation(summary = "Validar codigo de recuperacao", description = "Valida o codigo de recuperacao de senha antes da redefinicao da senha")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Codigo validado com sucesso", content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Codigo invalido ou expirado", content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "429", description = "Muitas tentativas", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<MessageResponse> verifyResetCode(@Valid @RequestBody VerifyResetPasswordCodeRequest request) {
        authService.verifyResetPasswordCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(new MessageResponse("Codigo validado com sucesso"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Redefinir senha", description = "Redefine a senha do usuario usando o codigo de recuperacao enviado por e-mail")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Senha redefinida com sucesso", content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Codigo invalido ou senhas nao coincidem", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
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
    @Operation(summary = "Reenviar codigo de recuperacao", description = "Reenvia o codigo de recuperacao de senha para o e-mail do usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Codigo reenviado com sucesso", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<MessageResponse> resendResetCode(@RequestParam String email) {
        authService.forgotPassword(email);
        return ResponseEntity.ok(new MessageResponse("Codigo de recuperacao reenviado"));
    }

}

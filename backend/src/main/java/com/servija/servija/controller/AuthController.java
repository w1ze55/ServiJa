package com.servija.servija.controller;

import com.servija.servija.dto.AuthResponse;
import com.servija.servija.dto.AdminResetPasswordLinkResetRequest;
import com.servija.servija.dto.AdminResetPasswordLinkValidateRequest;
import com.servija.servija.dto.AdminResetPasswordLinkValidationResponse;
import com.servija.servija.dto.AddressRequest;
import com.servija.servija.dto.ForgotPasswordRequest;
import com.servija.servija.dto.LoginRequest;
import com.servija.servija.dto.MessageResponse;
import com.servija.servija.dto.RegistrationValidationRequest;
import com.servija.servija.dto.RegisterRequest;
import com.servija.servija.dto.ResetPasswordRequest;
import com.servija.servija.dto.TwoFactorVerifyRequest;
import com.servija.servija.dto.UserRoleRequest;
import com.servija.servija.dto.VerifyEmailRequest;
import com.servija.servija.dto.VerifyResetPasswordCodeRequest;
import com.servija.servija.exception.TooManyAttemptsException;
import com.servija.servija.service.AdminResetPasswordLinkService;
import com.servija.servija.service.AuthService;
import com.servija.servija.service.RateLimiterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final AdminResetPasswordLinkService adminResetPasswordLinkService;
    private final RateLimiterService rateLimiter;

    private static final long WINDOW_5_MIN = 5 * 60 * 1000L;
    private static final long WINDOW_10_MIN = 10 * 60 * 1000L;
    private static final long WINDOW_1_HOUR = 60 * 60 * 1000L;

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * H5: aplica limite por IP e (opcionalmente) por identificador (e-mail). Lança
     * 429 (TooManyAttemptsException) quando qualquer um dos limites é excedido.
     */
    private void rateLimit(String action, HttpServletRequest request, String identifier,
            int maxPerIp, int maxPerId, long windowMillis) {
        boolean ipOk = rateLimiter.tryConsume(
                action + ":ip:" + clientIp(request), maxPerIp, windowMillis);
        boolean idOk = identifier == null || identifier.isBlank()
                || rateLimiter.tryConsume(
                        action + ":id:" + identifier.trim().toLowerCase(), maxPerId, windowMillis);
        if (!ipOk || !idOk) {
            throw new TooManyAttemptsException(
                    "Muitas tentativas. Aguarde alguns minutos e tente novamente.", 0, null);
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar novo usuario", description = "Cria uma nova conta de usuario no sistema. Apos o registro, um codigo de verificacao e enviado para o e-mail fornecido.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario registrado com sucesso", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados invalidos ou e-mail ja cadastrado", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        rateLimit("register", httpRequest, request.getEmail(), 10, 5, WINDOW_1_HOUR);
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
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        rateLimit("login", httpRequest, request.getEmail(), 20, 8, WINDOW_5_MIN);
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/login")
    @Operation(summary = "Login do painel administrativo", description = "Autentica apenas usuarios com perfil ADMIN. Demais tipos recebem erro de credenciais.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais invalidas ou usuario nao administrador", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<AuthResponse> loginAdmin(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        rateLimit("admin-login", httpRequest, request.getEmail(), 20, 8, WINDOW_5_MIN);
        AuthResponse response = authService.loginAdmin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/2fa/verify")
    @Operation(summary = "Concluir login com 2FA", description = "Valida o codigo TOTP ou backup code e retorna o JWT definitivo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Codigo validado, login concluido", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Codigo ou token invalido", content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "429", description = "Muitas tentativas", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<AuthResponse> verifyTwoFactor(@Valid @RequestBody TwoFactorVerifyRequest request,
            HttpServletRequest httpRequest) {
        rateLimit("2fa", httpRequest, null, 30, 0, WINDOW_5_MIN);
        AuthResponse response = authService.verifyTwoFactorChallenge(request.getChallengeToken(), request.getCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/switch-role")
    @Operation(summary = "Alternar perfil ativo", description = "Atualiza o papel ativo do usuario autenticado e retorna um novo token JWT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil ativo alterado com sucesso", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Perfil invalido ou nao habilitado", content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<AuthResponse> switchRole(@Valid @RequestBody UserRoleRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AuthResponse response = authService.switchRole(authentication.getName(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verificar e-mail", description = "Verifica o e-mail do usuario usando o codigo enviado durante o registro e retorna um token JWT para autenticacao automatica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "E-mail verificado com sucesso", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Codigo invalido ou expirado", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request,
            HttpServletRequest httpRequest) {
        rateLimit("verify-email", httpRequest, request.getEmail(), 40, 15, WINDOW_10_MIN);
        AuthResponse response = authService.verifyEmail(request.getEmail(), request.getCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Reenviar codigo de verificacao", description = "Reenvia o codigo de verificacao de e-mail para o usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Codigo reenviado com sucesso", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<MessageResponse> resendVerification(@RequestParam String email,
            HttpServletRequest httpRequest) {
        rateLimit("resend-verify", httpRequest, email, 15, 3, WINDOW_10_MIN);
        authService.resendVerificationCode(email);
        return ResponseEntity.ok(new MessageResponse("Codigo de verificacao reenviado"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar recuperacao de senha", description = "Inicia o processo de recuperacao de senha enviando um codigo para o e-mail do usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Codigo de recuperacao enviado", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        rateLimit("forgot", httpRequest, request.getEmail(), 15, 3, WINDOW_10_MIN);
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
    public ResponseEntity<MessageResponse> verifyResetCode(@Valid @RequestBody VerifyResetPasswordCodeRequest request,
            HttpServletRequest httpRequest) {
        rateLimit("verify-reset", httpRequest, request.getEmail(), 40, 15, WINDOW_10_MIN);
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
    public ResponseEntity<MessageResponse> resendResetCode(@RequestParam String email,
            HttpServletRequest httpRequest) {
        rateLimit("resend-reset", httpRequest, email, 15, 3, WINDOW_10_MIN);
        authService.forgotPassword(email);
        return ResponseEntity.ok(new MessageResponse("Codigo de recuperacao reenviado"));
    }

    @PostMapping("/admin-reset-link/validate")
    @Operation(summary = "Validar link de reset do admin", description = "Valida um link unico de redefinicao de senha enviado pelo painel administrativo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Link valido", content = @Content(schema = @Schema(implementation = AdminResetPasswordLinkValidationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Link invalido, expirado ou ja utilizado", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<AdminResetPasswordLinkValidationResponse> validateAdminResetLink(
            @Valid @RequestBody AdminResetPasswordLinkValidateRequest request) {
        return ResponseEntity.ok(adminResetPasswordLinkService.validateToken(request.getToken()));
    }

    @PostMapping("/admin-reset-link/reset-password")
    @Operation(summary = "Redefinir senha por link do admin", description = "Redefine a senha usando um link unico enviado por um administrador")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Senha redefinida com sucesso", content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Link invalido, expirado, ja utilizado ou senha invalida", content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<MessageResponse> resetPasswordByAdminLink(
            @Valid @RequestBody AdminResetPasswordLinkResetRequest request) {
        adminResetPasswordLinkService.resetPassword(
                request.getToken(),
                request.getNewPassword(),
                request.getConfirmPassword());
        return ResponseEntity.ok(new MessageResponse("Senha redefinida com sucesso"));
    }

}

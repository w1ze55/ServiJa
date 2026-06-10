package com.servija.servija.config;

import com.servija.servija.repository.ConversationRepository;
import com.servija.servija.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final ConversationRepository conversationRepository;

    /** Topicos cujo ultimo segmento e o e-mail do dono; so o proprio dono pode assinar. */
    private static final List<String> EMAIL_SCOPED_PREFIXES = List.of(
            "/topic/payment/",
            "/topic/service-request/",
            "/topic/notifications/",
            "/topic/profile-update/");

    /** Topicos de chat: /topic/chat/{conversationId} e /topic/chat/{conversationId}/read. */
    private static final String CHAT_PREFIX = "/topic/chat/";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscribe(accessor);
        }

        return message;
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        String jwt = extractToken(accessor);

        if (jwt != null) {
            try {
                String userEmail = jwtService.extractUsername(jwt);

                if (jwtService.isTotpChallengeToken(jwt)) {
                    // O token de desafio pré-2FA não autentica nada — bloqueia o bypass de 2FA.
                    log.warn("⚠️ Token de desafio 2FA rejeitado no WebSocket CONNECT");
                } else if (userEmail != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                    if (userDetails.isEnabled()
                            && jwtService.isTokenValid(jwt, userDetails)
                            && isTokenVersionValid(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());

                        accessor.setUser(authentication);
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        log.info("✅ WebSocket autenticado para usuário: {}", userEmail);
                    } else {
                        log.warn("⚠️ Token JWT inválido para WebSocket");
                    }
                }
            } catch (Exception e) {
                log.error("❌ Erro ao autenticar WebSocket: {}", e.getMessage());
            }
        }

        // H9: exige autenticação para qualquer conexão WebSocket (sem anônimos).
        if (accessor.getUser() == null) {
            log.warn("⛔ WebSocket CONNECT rejeitado: sem autenticação válida");
            throw new AccessDeniedException("Autenticação obrigatória para conectar ao WebSocket");
        }
    }

    private boolean isTokenVersionValid(String jwt, UserDetails userDetails) {
        if (!(userDetails instanceof com.servija.servija.model.User user)) {
            return true;
        }
        int current = user.getTokenVersion() != null ? user.getTokenVersion() : 0;
        Integer claimVersion = jwtService.extractTokenVersion(jwt);
        int presented = claimVersion != null ? claimVersion : 0;
        return presented == current;
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        String destination = accessor.getDestination();

        if (!isAuthorizedToSubscribe(user, destination)) {
            log.warn("⛔ Subscribe negado: usuario={} destino={}",
                    user != null ? user.getName() : "anon", destination);
            throw new AccessDeniedException("Nao autorizado a assinar este topico");
        }
    }

    /**
     * Autoriza a assinatura de um topico. Topicos por e-mail exigem que o e-mail no
     * destino seja exatamente o do usuario autenticado; topicos de chat exigem que o
     * usuario seja participante da conversa. Qualquer outro destino e negado.
     */
    private boolean isAuthorizedToSubscribe(Principal user, String destination) {
        if (user == null || destination == null) {
            return false;
        }
        String email = user.getName();
        if (email == null || email.isEmpty()) {
            return false;
        }

        for (String prefix : EMAIL_SCOPED_PREFIXES) {
            if (destination.startsWith(prefix)) {
                String target = destination.substring(prefix.length());
                return email.equals(target);
            }
        }

        if (destination.startsWith(CHAT_PREFIX)) {
            String rest = destination.substring(CHAT_PREFIX.length());
            int slash = rest.indexOf('/');
            String conversationId = slash >= 0 ? rest.substring(0, slash) : rest;
            if (conversationId.isEmpty()) {
                return false;
            }
            return conversationRepository.findById(conversationId)
                    .map(c -> email.equals(c.getContractorEmail()) || email.equals(c.getProviderEmail()))
                    .orElse(false);
        }

        // Destino desconhecido: nega por padrao (fail-safe).
        return false;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        List<String> authorization = accessor.getNativeHeader("Authorization");
        if (authorization != null && !authorization.isEmpty()) {
            String authHeader = authorization.get(0);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }

        List<String> tokenHeader = accessor.getNativeHeader("token");
        if (tokenHeader != null && !tokenHeader.isEmpty()) {
            return tokenHeader.get(0);
        }

        return null;
    }
}

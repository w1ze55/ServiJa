package com.servija.servija.config;

import com.servija.servija.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Tentar obter token do header Authorization
            List<String> authorization = accessor.getNativeHeader("Authorization");
            
            String jwt = null;
            
            // Tentar Authorization header primeiro
            if (authorization != null && !authorization.isEmpty()) {
                String authHeader = authorization.get(0);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    jwt = authHeader.substring(7);
                }
            }
            
            // Se não encontrou no Authorization, tentar o header 'token'
            if (jwt == null) {
                List<String> tokenHeader = accessor.getNativeHeader("token");
                if (tokenHeader != null && !tokenHeader.isEmpty()) {
                    jwt = tokenHeader.get(0);
                }
            }

            if (jwt != null) {
                try {
                    String userEmail = jwtService.extractUsername(jwt);
                    
                    if (userEmail != null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                        
                        if (jwtService.isTokenValid(jwt, userDetails)) {
                            UsernamePasswordAuthenticationToken authentication = 
                                new UsernamePasswordAuthenticationToken(
                                    userDetails, 
                                    null, 
                                    userDetails.getAuthorities()
                                );
                            
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
            } else {
                log.debug("WebSocket conectado sem autenticação JWT");
            }
        }

        return message;
    }
}

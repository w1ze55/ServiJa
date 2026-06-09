package com.servija.servija.config;

import com.servija.servija.model.chat.Conversation;
import com.servija.servija.repository.ConversationRepository;
import com.servija.servija.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthChannelInterceptorTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private ConversationRepository conversationRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("preSend autentica CONNECT usando header Authorization")
    void preSendAuthenticatesUsingAuthorizationHeader() {
        WebSocketAuthChannelInterceptor interceptor = interceptor();
        UserDetails userDetails = new User("user@test.com", "secret", List.of());

        when(jwtService.extractUsername("valid-token")).thenReturn("user@test.com");
        when(jwtService.isTotpChallengeToken("valid-token")).thenReturn(false);
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid-token", userDetails)).thenReturn(true);

        Message<?> result = interceptor.preSend(
                connectMessage("Bearer valid-token", null),
                mock(MessageChannel.class)
        );

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(accessor).isNotNull();
        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo("user@test.com");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("preSend autentica CONNECT usando fallback no header token")
    void preSendAuthenticatesUsingTokenHeaderFallback() {
        WebSocketAuthChannelInterceptor interceptor = interceptor();
        UserDetails userDetails = new User("user@test.com", "secret", List.of());

        when(jwtService.extractUsername("fallback-token")).thenReturn("user@test.com");
        when(jwtService.isTotpChallengeToken("fallback-token")).thenReturn(false);
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("fallback-token", userDetails)).thenReturn(true);

        Message<?> result = interceptor.preSend(
                connectMessage(null, "fallback-token"),
                mock(MessageChannel.class)
        );

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(accessor).isNotNull();
        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo("user@test.com");
        verify(userDetailsService).loadUserByUsername("user@test.com");
    }

    @Test
    @DisplayName("preSend nao autentica quando o token JWT e invalido")
    void preSendSkipsAuthenticationWhenTokenIsInvalid() {
        WebSocketAuthChannelInterceptor interceptor = interceptor();
        UserDetails userDetails = new User("user@test.com", "secret", List.of());

        when(jwtService.extractUsername("invalid-token")).thenReturn("user@test.com");
        when(jwtService.isTotpChallengeToken("invalid-token")).thenReturn(false);
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("invalid-token", userDetails)).thenReturn(false);

        Message<?> result = interceptor.preSend(
                connectMessage("Bearer invalid-token", null),
                mock(MessageChannel.class)
        );

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(accessor).isNotNull();
        assertThat(accessor.getUser()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("preSend rejeita token de desafio 2FA no CONNECT")
    void preSendRejectsTotpChallengeToken() {
        WebSocketAuthChannelInterceptor interceptor = interceptor();

        when(jwtService.extractUsername("totp-token")).thenReturn("user@test.com");
        when(jwtService.isTotpChallengeToken("totp-token")).thenReturn(true);

        Message<?> result = interceptor.preSend(
                connectMessage("Bearer totp-token", null),
                mock(MessageChannel.class)
        );

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(accessor).isNotNull();
        assertThat(accessor.getUser()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername("user@test.com");
    }

    @Test
    @DisplayName("preSend permite SUBSCRIBE em topico scoped por email quando usuario e o dono")
    void preSendAllowsSubscribeToOwnEmailScopedTopic() {
        WebSocketAuthChannelInterceptor interceptor = interceptor();

        Message<?> result = interceptor.preSend(
                subscribeMessage("/topic/notifications/user@test.com", "user@test.com"),
                mock(MessageChannel.class)
        );

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(accessor).isNotNull();
        assertThat(accessor.getDestination()).isEqualTo("/topic/notifications/user@test.com");
    }

    @Test
    @DisplayName("preSend bloqueia SUBSCRIBE em topico scoped por email de outro usuario")
    void preSendBlocksSubscribeToForeignEmailScopedTopic() {
        WebSocketAuthChannelInterceptor interceptor = interceptor();

        assertThatThrownBy(() -> interceptor.preSend(
                subscribeMessage("/topic/notifications/other@test.com", "user@test.com"),
                mock(MessageChannel.class)
        )).isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Nao autorizado");
    }

    @Test
    @DisplayName("preSend permite SUBSCRIBE em topico de chat quando usuario participa da conversa")
    void preSendAllowsSubscribeToConversationTopicForParticipant() {
        WebSocketAuthChannelInterceptor interceptor = interceptor();
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(
                Conversation.builder()
                        .id("conv-1")
                        .contractorEmail("user@test.com")
                        .providerEmail("freela@test.com")
                        .build()
        ));

        Message<?> result = interceptor.preSend(
                subscribeMessage("/topic/chat/conv-1", "user@test.com"),
                mock(MessageChannel.class)
        );

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(accessor).isNotNull();
        assertThat(accessor.getDestination()).isEqualTo("/topic/chat/conv-1");
    }

    @Test
    @DisplayName("preSend permite SUBSCRIBE em read topic do chat quando usuario participa da conversa")
    void preSendAllowsSubscribeToConversationReadTopicForParticipant() {
        WebSocketAuthChannelInterceptor interceptor = interceptor();
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(
                Conversation.builder()
                        .id("conv-1")
                        .contractorEmail("client@test.com")
                        .providerEmail("user@test.com")
                        .build()
        ));

        Message<?> result = interceptor.preSend(
                subscribeMessage("/topic/chat/conv-1/read", "user@test.com"),
                mock(MessageChannel.class)
        );

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(accessor).isNotNull();
        assertThat(accessor.getDestination()).isEqualTo("/topic/chat/conv-1/read");
    }

    @Test
    @DisplayName("preSend bloqueia SUBSCRIBE em topico de chat quando usuario nao participa da conversa")
    void preSendBlocksSubscribeToConversationTopicForNonParticipant() {
        WebSocketAuthChannelInterceptor interceptor = interceptor();
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(
                Conversation.builder()
                        .id("conv-1")
                        .contractorEmail("client@test.com")
                        .providerEmail("freela@test.com")
                        .build()
        ));

        assertThatThrownBy(() -> interceptor.preSend(
                subscribeMessage("/topic/chat/conv-1", "user@test.com"),
                mock(MessageChannel.class)
        )).isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Nao autorizado");
    }

    @Test
    @DisplayName("preSend bloqueia SUBSCRIBE em destino desconhecido")
    void preSendBlocksSubscribeToUnknownDestination() {
        WebSocketAuthChannelInterceptor interceptor = interceptor();

        assertThatThrownBy(() -> interceptor.preSend(
                subscribeMessage("/topic/unknown/resource", "user@test.com"),
                mock(MessageChannel.class)
        )).isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Nao autorizado");
    }

    private WebSocketAuthChannelInterceptor interceptor() {
        return new WebSocketAuthChannelInterceptor(jwtService, userDetailsService, conversationRepository);
    }

    private Message<byte[]> connectMessage(String authorizationHeader, String tokenHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId("session-1");
        accessor.setLeaveMutable(true);

        if (authorizationHeader != null) {
            accessor.addNativeHeader("Authorization", authorizationHeader);
        }

        if (tokenHeader != null) {
            accessor.addNativeHeader("token", tokenHeader);
        }

        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> subscribeMessage(String destination, String username) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionId("session-1");
        accessor.setDestination(destination);
        accessor.setUser(new UsernamePasswordAuthenticationToken(username, null, List.of()));
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}

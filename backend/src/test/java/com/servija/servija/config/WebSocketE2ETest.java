package com.servija.servija.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servija.servija.dto.NotificationStreamEvent;
import com.servija.servija.service.JwtService;
import com.servija.servija.service.WebSocketService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport;
import org.springframework.web.socket.sockjs.client.SockJsClient;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = WebSocketE2ETest.TestApplication.class,
        properties = {
                "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "jwt.expiration=600000",
                "cors.allowed-origins=http://localhost",
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
                        "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration," +
                        "org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration," +
                        "org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration," +
                        "org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration," +
                        "org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration"
        }
)
class WebSocketE2ETest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            SecurityConfig.class,
            WebSocketConfig.class,
            JwtService.class,
            JwtAuthenticationFilter.class,
            WebSocketAuthChannelInterceptor.class,
            WebSocketService.class
    })
    static class TestApplication {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        UserDetailsService userDetailsService() {
            return username -> User.withUsername(username)
                    .password("{noop}secret")
                    .authorities("ROLE_USER")
                    .build();
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private SimpUserRegistry simpUserRegistry;

    private WebSocketStompClient stompClient;
    private StompSession session;

    @BeforeEach
    void setUp() {
        SockJsClient sockJsClient = new SockJsClient(
                List.of(new RestTemplateXhrTransport(new RestTemplate()))
        );
        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @AfterEach
    void tearDown() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
        if (stompClient != null) {
            stompClient.stop();
        }
    }

    @Test
    @DisplayName("cliente autenticado via SockJS XHR recebe evento no topico de notificacoes")
    void authenticatedSockJsClientReceivesNotificationEvent() throws Exception {
        UserDetails userDetails = User.withUsername("user@test.com")
                .password("secret")
                .authorities("ROLE_USER")
                .build();
        String token = jwtService.generateToken(userDetails, Map.of(), 60_000L);

        BlockingQueue<NotificationStreamEvent> receivedEvents = new LinkedBlockingQueue<>();
        CompletableFuture<Throwable> transportError = new CompletableFuture<>();
        CompletableFuture<Void> subscribed = new CompletableFuture<>();

        StompSessionHandlerAdapter sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession connectedSession, StompHeaders connectedHeaders) {
                connectedSession.subscribe("/topic/notifications/user@test.com", new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return NotificationStreamEvent.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        receivedEvents.offer((NotificationStreamEvent) payload);
                    }
                });
                subscribed.complete(null);
            }

            @Override
            public void handleException(
                    StompSession connectedSession,
                    StompCommand command,
                    StompHeaders headers,
                    byte[] payload,
                    Throwable exception
            ) {
                transportError.complete(exception);
            }

            @Override
            public void handleTransportError(StompSession connectedSession, Throwable exception) {
                transportError.complete(exception);
            }
        };

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        session = stompClient.connectAsync(
                "http://localhost:" + port + "/ws",
                new WebSocketHttpHeaders(),
                connectHeaders,
                sessionHandler
        ).get(10, TimeUnit.SECONDS);

        subscribed.get(10, TimeUnit.SECONDS);
        awaitCondition(
                Duration.ofSeconds(5),
                () -> simpUserRegistry.getUser("user@test.com") != null
        );

        NotificationStreamEvent event = NotificationStreamEvent.builder()
                .eventType("CREATED")
                .notificationId(777L)
                .unreadCount(4L)
                .build();

        Thread.sleep(200);
        webSocketService.publishNotificationEvent("user@test.com", event);

        NotificationStreamEvent received = receivedEvents.poll(10, TimeUnit.SECONDS);
        assertThat(transportError.isDone()).isFalse();
        assertThat(received).isNotNull();
        assertThat(received.getEventType()).isEqualTo("CREATED");
        assertThat(received.getNotificationId()).isEqualTo(777L);
        assertThat(received.getUnreadCount()).isEqualTo(4L);
        assertThat(simpUserRegistry.getUser("user@test.com")).isNotNull();
    }

    private void awaitCondition(Duration timeout, BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(100);
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }
}

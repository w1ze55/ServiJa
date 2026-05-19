package com.servija.servija.controller;

import com.servija.servija.dto.chat.ConversationDTO;
import com.servija.servija.dto.chat.MessageDTO;
import com.servija.servija.dto.chat.SendMessageRequest;
import com.servija.servija.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Mensagens entre contratante e freelancer")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/conversations")
    @Operation(summary = "Lista conversas do usuário autenticado")
    public ResponseEntity<List<ConversationDTO>> listConversations(Authentication auth) {
        return ResponseEntity.ok(chatService.listConversations(auth.getName()));
    }

    @GetMapping("/service-request/{serviceRequestId}")
    @Operation(summary = "Obtém ou cria conversa para uma solicitação de serviço")
    public ResponseEntity<ConversationDTO> getOrCreateConversation(
            @PathVariable Long serviceRequestId,
            Authentication auth) {
        return ResponseEntity.ok(chatService.getOrCreateConversation(serviceRequestId, auth.getName()));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Busca mensagens de uma conversa (paginado, ordem cronológica)")
    public ResponseEntity<Page<MessageDTO>> getMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication auth) {
        return ResponseEntity.ok(chatService.getMessages(conversationId, auth.getName(), page, size));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Envia uma mensagem")
    public ResponseEntity<MessageDTO> sendMessage(
            @PathVariable String conversationId,
            @Valid @RequestBody SendMessageRequest request,
            Authentication auth) {
        return ResponseEntity.ok(chatService.sendMessage(conversationId, auth.getName(), request));
    }

    @PutMapping("/conversations/{conversationId}/read")
    @Operation(summary = "Marca mensagens como lidas")
    public ResponseEntity<Void> markAsRead(
            @PathVariable String conversationId,
            Authentication auth) {
        chatService.markAsRead(conversationId, auth.getName());
        return ResponseEntity.noContent().build();
    }
}

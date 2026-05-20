package com.servija.servija.repository;

import com.servija.servija.model.chat.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MessageRepository extends MongoRepository<Message, String> {
    Page<Message> findByConversationIdOrderBySentAtAsc(String conversationId, Pageable pageable);
}

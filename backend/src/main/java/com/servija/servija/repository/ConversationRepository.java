package com.servija.servija.repository;

import com.servija.servija.model.chat.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {
    Optional<Conversation> findByServiceRequestId(Long serviceRequestId);
    List<Conversation> findByContractorEmailOrProviderEmailOrderByUpdatedAtDesc(String contractorEmail, String providerEmail);
}

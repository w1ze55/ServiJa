package com.servija.servija.repository;

import com.servija.servija.model.Notification;
import com.servija.servija.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);

    long countByRecipientIdAndIsReadFalse(Long recipientId);

    boolean existsByRecipientIdAndTypeAndMetadataJson(Long recipientId, NotificationType type, String metadataJson);

    long deleteByIdAndRecipientId(Long id, Long recipientId);

    long deleteByRecipientId(Long recipientId);
}

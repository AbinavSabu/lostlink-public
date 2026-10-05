package com.yourteam.lostfound.repository;

import com.yourteam.lostfound.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.recipient.id = :recipientId")
    void deleteByRecipientId(@Param("recipientId") Long recipientId);
}
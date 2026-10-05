package com.yourteam.lostfound.repository;

import com.yourteam.lostfound.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // 1-on-1 Scoped Thread: item + specific claimant/inquirer
    List<Message> findByItemIdAndClaimantIdOrderBySentAtAsc(Long itemId, Long claimantId);

    // Fallback or full item thread
    List<Message> findByItemIdOrderBySentAtAsc(Long itemId);

    // Only unread messages directed at this user
    List<Message> findByRecipientIdAndIsReadFalseOrderBySentAtDesc(Long recipientId);

    // List all distinct claimant IDs who have messaged about this item
    @Query("SELECT DISTINCT m.claimantId FROM Message m WHERE m.itemId = :itemId AND m.claimantId IS NOT NULL")
    List<Long> findDistinctClaimantIdsByItemId(@Param("itemId") Long itemId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Message m WHERE m.itemId = :itemId AND (m.senderId = :userId OR m.recipientId = :userId OR m.claimantId = :userId)")
    boolean isParticipantInItemMessages(@Param("itemId") Long itemId, @Param("userId") Long userId);

    // Mark 1-on-1 scoped thread as read
    @Transactional
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.itemId = :itemId AND m.claimantId = :claimantId AND m.recipientId = :recipientId AND m.isRead = false")
    void markScopedThreadAsRead(@Param("itemId") Long itemId, @Param("claimantId") Long claimantId, @Param("recipientId") Long recipientId);

    @Transactional
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.itemId = :itemId AND m.recipientId = :recipientId AND m.isRead = false")
    void markThreadAsRead(@Param("itemId") Long itemId, @Param("recipientId") Long recipientId);

    @Transactional
    @Modifying
    @Query("DELETE FROM Message m WHERE m.itemId = :itemId")
    void deleteByItemId(@Param("itemId") Long itemId);

    @Transactional
    @Modifying
    @Query("DELETE FROM Message m WHERE m.senderId = :userId OR m.recipientId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
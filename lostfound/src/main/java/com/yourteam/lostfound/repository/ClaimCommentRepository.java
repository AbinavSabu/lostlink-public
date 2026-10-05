package com.yourteam.lostfound.repository;

import com.yourteam.lostfound.model.ClaimComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ClaimCommentRepository extends JpaRepository<ClaimComment, Long> {
    List<ClaimComment> findByClaimIdOrderByCreatedAtAsc(Long claimId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ClaimComment cc WHERE cc.claim.item.id = :itemId")
    void deleteByClaimItemId(@Param("itemId") Long itemId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ClaimComment cc WHERE cc.author.id = :authorId")
    void deleteByAuthorId(@Param("authorId") Long authorId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ClaimComment cc WHERE cc.claim.claimant.id = :claimantId")
    void deleteByClaimClaimantId(@Param("claimantId") Long claimantId);
}
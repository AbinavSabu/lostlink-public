package com.yourteam.lostfound.repository;

import com.yourteam.lostfound.model.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
            "FROM Claim c WHERE c.item.id = :itemId AND c.claimant.id = :claimantId AND UPPER(c.status) = UPPER(:status)")
    boolean existsByItemIdAndClaimantIdAndStatus(
            @Param("itemId") Long itemId,
            @Param("claimantId") Long claimantId,
            @Param("status") String status
    );

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
            "FROM Claim c WHERE c.item.id = :itemId AND c.claimant.id = :claimantId")
    boolean existsByItemIdAndClaimantId(
            @Param("itemId") Long itemId,
            @Param("claimantId") Long claimantId
    );

    @Query("SELECT c FROM Claim c WHERE c.item.id = :itemId AND c.id <> :claimId")
    List<Claim> findByItemIdAndIdNot(
            @Param("itemId") Long itemId,
            @Param("claimId") Long claimId
    );

    @Query("SELECT c FROM Claim c WHERE c.item.user.id = :userId ORDER BY c.createdAt DESC")
    List<Claim> findByItemUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT DISTINCT c FROM Claim c LEFT JOIN FETCH c.item LEFT JOIN FETCH c.claimant ORDER BY c.createdAt DESC")
    List<Claim> findAllWithDetails();

    @Query("SELECT COUNT(c) FROM Claim c WHERE UPPER(c.status) = UPPER(:status)")
    long countByStatusIgnoreCase(@Param("status") String status);

    @Modifying
    @Transactional
    @Query("DELETE FROM Claim c WHERE c.item.id = :itemId")
    void deleteByItemId(@Param("itemId") Long itemId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Claim c WHERE c.claimant.id = :claimantId")
    void deleteByClaimantId(@Param("claimantId") Long claimantId);
}
package com.yourteam.lostfound.service;

import com.yourteam.lostfound.dto.ClaimRequestDTO;
import com.yourteam.lostfound.dto.ClaimResponseDTO;
import com.yourteam.lostfound.dto.ClaimCommentResponseDTO;
import com.yourteam.lostfound.dto.ClaimCommentRequestDTO;
import java.util.List;


public interface ClaimService {
    List<ClaimResponseDTO> getMyClaims();
    List<ClaimResponseDTO> getReceivedClaims();
    ClaimCommentResponseDTO addComment(Long claimId, ClaimCommentRequestDTO dto);
    List<ClaimCommentResponseDTO> getCommentsByClaimId(Long claimId);
    ClaimResponseDTO submitClaim(ClaimRequestDTO claimRequestDTO);
    com.yourteam.lostfound.dto.ClaimAnalyticsDTO getClaimAnalytics();
    byte[] exportClaimsToCsv();

    ClaimResponseDTO getClaimById(Long id);
    ClaimResponseDTO verifyHandoverPin(Long itemId, String pin);

    List<ClaimResponseDTO> getAllClaims();

    List<ClaimResponseDTO> getClaimsByItemId(Long itemId);

    List<ClaimResponseDTO> getClaimsByClaimantId(Long claimantId);

    ClaimResponseDTO updateClaimStatus(Long id, String status);

    void deleteClaim(Long id);

    ClaimResponseDTO cancelClaim(Long id);

    void reopenItem(Long itemId);
}
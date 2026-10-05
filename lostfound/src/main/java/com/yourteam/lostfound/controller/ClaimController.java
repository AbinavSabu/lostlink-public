package com.yourteam.lostfound.controller;

import com.yourteam.lostfound.dto.ClaimCommentRequestDTO;
import com.yourteam.lostfound.dto.ClaimCommentResponseDTO;
import com.yourteam.lostfound.dto.ClaimRequestDTO;
import com.yourteam.lostfound.dto.ClaimResponseDTO;
import com.yourteam.lostfound.service.ClaimService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {
    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @PostMapping
    public ResponseEntity<ClaimResponseDTO> createClaim(@RequestBody ClaimRequestDTO requestDTO) {
        ClaimResponseDTO response = claimService.submitClaim(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/notifications")
    public ResponseEntity<Map<String, Object>> getClaimNotifications() {
        List<ClaimResponseDTO> receivedClaims = claimService.getReceivedClaims();

        List<ClaimResponseDTO> pendingClaims = receivedClaims.stream()
                .filter(c -> "PENDING".equalsIgnoreCase(c.getStatus()))
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("count", (long) pendingClaims.size());
        response.put("notifications", pendingClaims);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/received")
    public ResponseEntity<List<ClaimResponseDTO>> getReceivedClaims() {
        return ResponseEntity.ok(claimService.getReceivedClaims());
    }

    @GetMapping("/my-claims")
    public ResponseEntity<List<ClaimResponseDTO>> getMyClaims() {
        return ResponseEntity.ok(claimService.getMyClaims());
    }

    @GetMapping("/item/{itemId}")
    public ResponseEntity<List<ClaimResponseDTO>> getClaimsByItemId(@PathVariable Long itemId) {
        return ResponseEntity.ok(claimService.getClaimsByItemId(itemId));
    }

    @GetMapping("/claimant/{claimantId}")
    public ResponseEntity<List<ClaimResponseDTO>> getClaimsByClaimantId(@PathVariable Long claimantId) {
        return ResponseEntity.ok(claimService.getClaimsByClaimantId(claimantId));
    }

    // Physical Handover PIN Verification
    @PostMapping("/item/{itemId}/verify-pin")
    public ResponseEntity<ClaimResponseDTO> verifyHandoverPin(
            @PathVariable Long itemId,
            @RequestBody Map<String, String> request) {
        String pin = request.get("pin");
        return ResponseEntity.ok(claimService.verifyHandoverPin(itemId, pin));
    }

    // Dynamic ID route placed after explicit sub-routes
    @GetMapping("/{id}")
    public ResponseEntity<ClaimResponseDTO> getClaimById(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.getClaimById(id));
    }

    @GetMapping
    public ResponseEntity<List<ClaimResponseDTO>> getAllClaims(
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) Long claimantId) {

        if (itemId != null) {
            return ResponseEntity.ok(claimService.getClaimsByItemId(itemId));
        }
        if (claimantId != null) {
            return ResponseEntity.ok(claimService.getClaimsByClaimantId(claimantId));
        }
        return ResponseEntity.ok(claimService.getAllClaims());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ClaimResponseDTO> updateClaimStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(claimService.updateClaimStatus(id, status));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ClaimResponseDTO> cancelClaim(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.cancelClaim(id));
    }

    @PatchMapping("/item/{itemId}/reopen")
    public ResponseEntity<Void> reopenItem(@PathVariable Long itemId) {
        claimService.reopenItem(itemId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteClaim(@PathVariable Long id) {
        claimService.deleteClaim(id);
        return ResponseEntity.ok("Claim deleted successfully with id: " + id);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<ClaimCommentResponseDTO> addComment(
            @PathVariable Long id,
            @RequestBody ClaimCommentRequestDTO requestDTO) {
        return new ResponseEntity<>(claimService.addComment(id, requestDTO), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<ClaimCommentResponseDTO>> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.getCommentsByClaimId(id));
    }
}
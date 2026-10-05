package com.yourteam.lostfound.controller;

import com.yourteam.lostfound.dto.AdminStatsDTO;
import com.yourteam.lostfound.dto.ClaimAnalyticsDTO;
import com.yourteam.lostfound.dto.ItemResponseDTO;
import com.yourteam.lostfound.repository.ClaimRepository;
import com.yourteam.lostfound.repository.UserRepository;
import com.yourteam.lostfound.service.AdminService;
import com.yourteam.lostfound.service.ClaimService;
import com.yourteam.lostfound.service.ItemService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ItemService itemService;
    private final AdminService adminService;
    private final ClaimService claimService;
    private final UserRepository userRepository;
    private final ClaimRepository claimRepository;

    public AdminController(ItemService itemService,
                           AdminService adminService,
                           ClaimService claimService,
                           UserRepository userRepository,
                           ClaimRepository claimRepository) {
        this.itemService = itemService;
        this.adminService = adminService;
        this.claimService = claimService;
        this.userRepository = userRepository;
        this.claimRepository = claimRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDTO> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/items")
    public ResponseEntity<List<ItemResponseDTO>> getAllItemsForAdmin() {
        return ResponseEntity.ok(itemService.getAllItems());
    }

    @GetMapping("/claims")
    public ResponseEntity<List<Map<String, Object>>> getAllClaimsForAdmin() {
        List<Map<String, Object>> claims = claimRepository.findAllWithDetails().stream()
                .map(claim -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", claim.getId());
                    map.put("status", claim.getStatus());
                    map.put("proofDescription", claim.getProofDescription());
                    map.put("verificationAnswer", claim.getVerificationAnswer());
                    map.put("createdAt", claim.getCreatedAt());

                    if (claim.getClaimant() != null) {
                        map.put("claimantId", claim.getClaimant().getId());
                        map.put("claimantName", claim.getClaimant().getName());
                        map.put("claimantEmail", claim.getClaimant().getEmail());
                    }

                    if (claim.getItem() != null) {
                        map.put("itemId", claim.getItem().getId());
                        map.put("itemTitle", claim.getItem().getTitle());
                        map.put("itemStatus", claim.getItem().getStatus());
                        map.put("itemCategory", claim.getItem().getCategory());
                    }

                    return map;
                })
                .toList();
        return ResponseEntity.ok(claims);
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsersForAdmin() {
        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(user -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", user.getId());
                    map.put("name", user.getName());
                    map.put("email", user.getEmail());
                    map.put("role", user.getRole());
                    return map;
                })
                .toList();
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/items/{id}/status")
    public ResponseEntity<ItemResponseDTO> updateItemStatusAdmin(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(itemService.updateItemStatus(id, status));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItemAdmin(@PathVariable Long id) {
        adminService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUserAdmin(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        adminService.deleteUserSafely(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/claims/{id}/approve")
    public ResponseEntity<Void> approveClaimAdmin(@PathVariable Long id) {
        adminService.approveClaim(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/claims/{id}/reject")
    public ResponseEntity<Void> rejectClaimAdmin(@PathVariable Long id) {
        adminService.rejectClaim(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/claims/analytics")
    public ResponseEntity<ClaimAnalyticsDTO> getClaimAnalytics() {
        return ResponseEntity.ok(claimService.getClaimAnalytics());
    }

    @GetMapping(value = "/claims/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportClaims() {
        byte[] csvData = claimService.exportClaimsToCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"claims-audit-report.csv\"")
                .body(csvData);
    }
}
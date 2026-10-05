package com.yourteam.lostfound.service;

import com.yourteam.lostfound.dto.AdminStatsDTO;

public interface AdminService {
    AdminStatsDTO getDashboardStats();
    void deleteItem(Long itemId);
    void approveClaim(Long claimId);
    void rejectClaim(Long claimId);
    void deleteUserSafely(Long userId);
}
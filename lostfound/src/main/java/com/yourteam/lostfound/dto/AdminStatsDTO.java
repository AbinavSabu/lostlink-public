package com.yourteam.lostfound.dto;

public class AdminStatsDTO {
    private long totalItems;
    private long lostItems;
    private long foundItems;
    private long claimedItems;
    private long totalClaims;
    private long pendingClaims;
    private long approvedClaims;
    private long totalUsers;

    public AdminStatsDTO() {}

    public AdminStatsDTO(long totalItems, long lostItems, long foundItems, long claimedItems,
                         long totalClaims, long pendingClaims, long approvedClaims, long totalUsers) {
        this.totalItems = totalItems;
        this.lostItems = lostItems;
        this.foundItems = foundItems;
        this.claimedItems = claimedItems;
        this.totalClaims = totalClaims;
        this.pendingClaims = pendingClaims;
        this.approvedClaims = approvedClaims;
        this.totalUsers = totalUsers;
    }

    public long getTotalItems() { return totalItems; }
    public long getLostItems() { return lostItems; }
    public long getFoundItems() { return foundItems; }
    public long getClaimedItems() { return claimedItems; }
    public long getTotalClaims() { return totalClaims; }
    public long getPendingClaims() { return pendingClaims; }
    public long getApprovedClaims() { return approvedClaims; }
    public long getTotalUsers() { return totalUsers; }
}
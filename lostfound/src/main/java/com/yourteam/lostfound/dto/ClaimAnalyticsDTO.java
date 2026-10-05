package com.yourteam.lostfound.dto;

public class ClaimAnalyticsDTO {
    private long totalClaims;
    private long pendingClaims;
    private long approvedClaims;
    private long rejectedClaims;
    private double resolutionRate;
    private double rejectionRate;
    private double averageResolutionHours;

    public ClaimAnalyticsDTO() {}

    public ClaimAnalyticsDTO(long totalClaims, long pendingClaims, long approvedClaims, long rejectedClaims,
                             double resolutionRate, double rejectionRate, double averageResolutionHours) {
        this.totalClaims = totalClaims;
        this.pendingClaims = pendingClaims;
        this.approvedClaims = approvedClaims;
        this.rejectedClaims = rejectedClaims;
        this.resolutionRate = resolutionRate;
        this.rejectionRate = rejectionRate;
        this.averageResolutionHours = averageResolutionHours;
    }

    public long getTotalClaims() { return totalClaims; }
    public void setTotalClaims(long totalClaims) { this.totalClaims = totalClaims; }

    public long getPendingClaims() { return pendingClaims; }
    public void setPendingClaims(long pendingClaims) { this.pendingClaims = pendingClaims; }

    public long getApprovedClaims() { return approvedClaims; }
    public void setApprovedClaims(long approvedClaims) { this.approvedClaims = approvedClaims; }

    public long getRejectedClaims() { return rejectedClaims; }
    public void setRejectedClaims(long rejectedClaims) { this.rejectedClaims = rejectedClaims; }

    public double getResolutionRate() { return resolutionRate; }
    public void setResolutionRate(double resolutionRate) { this.resolutionRate = resolutionRate; }

    public double getRejectionRate() { return rejectionRate; }
    public void setRejectionRate(double rejectionRate) { this.rejectionRate = rejectionRate; }

    public double getAverageResolutionHours() { return averageResolutionHours; }
    public void setAverageResolutionHours(double averageResolutionHours) { this.averageResolutionHours = averageResolutionHours; }
}
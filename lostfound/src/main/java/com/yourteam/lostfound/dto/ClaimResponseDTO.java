package com.yourteam.lostfound.dto;

import java.time.LocalDateTime;

public class ClaimResponseDTO {

    private Long id;
    private Long itemId;
    private String itemTitle;
    private Long claimantId;
    private String claimantName;
    private String claimantEmail;
    private String proofDescription;
    private String verificationAnswer;
    private String status;
    private LocalDateTime createdAt;
    private String handoverCode;
    private LocalDateTime resolvedAt;
    private String handoverPin;
    private boolean handoverVerified;
    private String proofImageUrl;

    public String getProofImageUrl() {
        return proofImageUrl;
    }

    public void setProofImageUrl(String proofImageUrl) {
        this.proofImageUrl = proofImageUrl;
    }

    public ClaimResponseDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public String getItemTitle() { return itemTitle; }
    public void setItemTitle(String itemTitle) { this.itemTitle = itemTitle; }

    public Long getClaimantId() { return claimantId; }
    public void setClaimantId(Long claimantId) { this.claimantId = claimantId; }

    public String getClaimantName() { return claimantName; }
    public void setClaimantName(String claimantName) { this.claimantName = claimantName; }

    public String getClaimantEmail() { return claimantEmail; }
    public void setClaimantEmail(String claimantEmail) { this.claimantEmail = claimantEmail; }

    public String getProofDescription() { return proofDescription; }
    public void setProofDescription(String proofDescription) { this.proofDescription = proofDescription; }

    public String getVerificationAnswer() { return verificationAnswer; }
    public void setVerificationAnswer(String verificationAnswer) { this.verificationAnswer = verificationAnswer; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isHandoverVerified() {
        return handoverVerified;
    }

    public void setHandoverVerified(boolean handoverVerified) {
        this.handoverVerified = handoverVerified;
    }
    public String getHandoverCode() {
        return handoverCode != null ? handoverCode : handoverPin;
    }

    public void setHandoverCode(String handoverCode) {
        this.handoverCode = handoverCode;
        this.handoverPin = handoverCode;
    }

    public String getHandoverPin() {
        return handoverPin != null ? handoverPin : handoverCode;
    }

    public void setHandoverPin(String handoverPin) {
        this.handoverPin = handoverPin;
        this.handoverCode = handoverPin;
    }


}
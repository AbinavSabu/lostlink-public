package com.yourteam.lostfound.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "claims")
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "claimant_id", nullable = false)
    private User claimant;

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(length = 1000)
    private String proofDescription;

    @Column(nullable = false)
    private String status;
    @Column(name = "handover_code", length = 6)
    private String handoverCode;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    @Column(name = "proof_image_url")
    private String proofImageUrl;

    private LocalDateTime createdAt;

    public Claim() {
        this.createdAt = LocalDateTime.now();
    }

    public Claim(User claimant, Item item, String proofDescription, String status) {
        this.claimant = claimant;
        this.item = item;
        this.proofDescription = proofDescription;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getClaimant() { return claimant; }
    public void setClaimant(User claimant) { this.claimant = claimant; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public String getProofDescription() { return proofDescription; }
    public void setProofDescription(String proofDescription) { this.proofDescription = proofDescription; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    @Column(name = "verification_answer", columnDefinition = "TEXT")
    private String verificationAnswer;
    @Column(name = "handover_pin", length = 6)
    private String handoverPin;

    @Column(name = "is_handover_verified")
    private Boolean handoverVerified = false;

    // Getter and Setter
    public String getVerificationAnswer() {
        return verificationAnswer;
    }

    public void setVerificationAnswer(String verificationAnswer) {
        this.verificationAnswer = verificationAnswer;
    }
    public String getHandoverCode() { return handoverCode; }
    public void setHandoverCode(String handoverCode) { this.handoverCode = handoverCode; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getHandoverPin() { return handoverPin; }
    public void setHandoverPin(String handoverPin) { this.handoverPin = handoverPin; }

    public boolean isHandoverVerified() { return handoverVerified != null && handoverVerified; }
    public void setHandoverVerified(Boolean handoverVerified) { this.handoverVerified = handoverVerified != null && handoverVerified; }
    @Column(name = "failed_pin_attempts")
    private Integer failedPinAttempts = 0;

    public int getFailedPinAttempts() { return failedPinAttempts != null ? failedPinAttempts : 0; }
    public void setFailedPinAttempts(Integer failedPinAttempts) { this.failedPinAttempts = failedPinAttempts != null ? failedPinAttempts : 0; }

    public String getProofImageUrl() {
        return proofImageUrl;
    }

    public void setProofImageUrl(String proofImageUrl) {
        this.proofImageUrl = proofImageUrl;
    }
}
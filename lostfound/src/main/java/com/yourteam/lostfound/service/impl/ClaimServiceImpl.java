package com.yourteam.lostfound.service.impl;

import com.yourteam.lostfound.dto.ClaimAnalyticsDTO;
import com.yourteam.lostfound.dto.ClaimCommentRequestDTO;
import com.yourteam.lostfound.dto.ClaimCommentResponseDTO;
import com.yourteam.lostfound.dto.ClaimRequestDTO;
import com.yourteam.lostfound.dto.ClaimResponseDTO;
import com.yourteam.lostfound.exception.BadRequestException;
import com.yourteam.lostfound.exception.ResourceNotFoundException;
import com.yourteam.lostfound.exception.UnauthorizedException;
import com.yourteam.lostfound.model.Claim;
import com.yourteam.lostfound.model.ClaimComment;
import com.yourteam.lostfound.model.Item;
import com.yourteam.lostfound.model.User;
import com.yourteam.lostfound.repository.ClaimCommentRepository;
import com.yourteam.lostfound.repository.ClaimRepository;
import com.yourteam.lostfound.repository.ItemRepository;
import com.yourteam.lostfound.service.ClaimService;
import com.yourteam.lostfound.service.NotificationService;
import com.yourteam.lostfound.service.UserService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ClaimServiceImpl implements ClaimService {

    private final ClaimRepository claimRepository;
    private final ItemRepository itemRepository;
    private final UserService userService;
    private final ClaimCommentRepository commentRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    public ClaimServiceImpl(ClaimRepository claimRepository,
                            ItemRepository itemRepository,
                            UserService userService,
                            ClaimCommentRepository commentRepository,
                            NotificationService notificationService,
                            SimpMessagingTemplate messagingTemplate) {
        this.claimRepository = claimRepository;
        this.itemRepository = itemRepository;
        this.userService = userService;
        this.commentRepository = commentRepository;
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public ClaimResponseDTO submitClaim(ClaimRequestDTO dto) {
        User claimant = userService.getCurrentAuthenticatedUser();

        Item item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", dto.getItemId()));

        if (item.getUser() != null && item.getUser().getId().equals(claimant.getId())) {
            throw new BadRequestException("You cannot submit a claim for an item you reported.");
        }

        if ("CLAIMED".equalsIgnoreCase(item.getStatus()) || "REUNITED".equalsIgnoreCase(item.getStatus())) {
            throw new BadRequestException("This item has already been resolved and claimed.");
        }

        boolean alreadyPending = claimRepository.existsByItemIdAndClaimantIdAndStatus(
                item.getId(), claimant.getId(), "PENDING"
        );
        if (alreadyPending) {
            throw new BadRequestException("You already have an active pending claim for this item.");
        }

        Claim claim = new Claim(claimant, item, dto.getProofDescription(), "PENDING");
        claim.setVerificationAnswer(dto.getVerificationAnswer());
        // Store proof image URL into database
        claim.setProofImageUrl(dto.getProofImageUrl());

        Claim savedClaim = claimRepository.save(claim);

        if (item.getUser() != null) {
            notificationService.sendNotification(
                    item.getUser(),
                    "New ownership claim submitted for '" + item.getTitle() + "'.",
                    item.getId()
            );
        }

        return mapToDTO(savedClaim);
    }

    private boolean isAdminUser(User user) {
        if (user == null || user.getRole() == null) return false;
        String r = user.getRole().toUpperCase();
        return "ADMIN".equals(r) || "ROLE_ADMIN".equals(r);
    }

    @Override
    public ClaimResponseDTO getClaimById(Long id) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "id", id));
        verifyClaimParticipant(claim, currentUser);
        return mapToDTO(claim);
    }

    @Override
    public List<ClaimResponseDTO> getAllClaims() {
        User currentUser = userService.getCurrentAuthenticatedUser();
        boolean isAdmin = isAdminUser(currentUser);

        if (isAdmin) {
            return claimRepository.findAll()
                    .stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
        }

        return claimRepository.findAll()
                .stream()
                .filter(claim -> {
                    boolean isClaimant = claim.getClaimant() != null && claim.getClaimant().getId().equals(currentUser.getId());
                    boolean isReporter = claim.getItem() != null && claim.getItem().getUser() != null
                            && claim.getItem().getUser().getId().equals(currentUser.getId());
                    return isClaimant || isReporter;
                })
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClaimResponseDTO> getClaimsByItemId(Long itemId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        boolean isAdmin = isAdminUser(currentUser);

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", itemId));

        boolean isReporter = item.getUser() != null && item.getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isReporter) {
            // A non-reporter student can only view their own claim for this item
            return claimRepository.findAll()
                    .stream()
                    .filter(claim -> claim.getItem() != null && claim.getItem().getId().equals(itemId))
                    .filter(claim -> claim.getClaimant() != null && claim.getClaimant().getId().equals(currentUser.getId()))
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
        }

        return claimRepository.findAll()
                .stream()
                .filter(claim -> claim.getItem() != null && claim.getItem().getId().equals(itemId))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClaimResponseDTO> getClaimsByClaimantId(Long claimantId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        boolean isAdmin = isAdminUser(currentUser);

        if (!isAdmin && !currentUser.getId().equals(claimantId)) {
            throw new UnauthorizedException("You are not authorized to view claims for this user.");
        }

        return claimRepository.findAll()
                .stream()
                .filter(claim -> claim.getClaimant() != null && claim.getClaimant().getId().equals(claimantId))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClaimResponseDTO> getMyClaims() {
        User currentUser = userService.getCurrentAuthenticatedUser();
        return claimRepository.findAll()
                .stream()
                .filter(claim -> claim.getClaimant() != null && claim.getClaimant().getId().equals(currentUser.getId()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClaimResponseDTO> getReceivedClaims() {
        User currentUser = userService.getCurrentAuthenticatedUser();
        return claimRepository.findByItemUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public ClaimResponseDTO updateClaimStatus(Long id, String status) {
        User currentUser = userService.getCurrentAuthenticatedUser();

        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "id", id));

        Item item = claim.getItem();

        boolean isAdmin = isAdminUser(currentUser);
        boolean isOwner = item != null && item.getUser() != null && item.getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new UnauthorizedException("Only the original item reporter or an admin can update this claim status.");
        }

        String normalizedStatus = status.toUpperCase();
        claim.setStatus(normalizedStatus);

        if ("APPROVED".equals(normalizedStatus) && item != null) {
            // Generate a secure 6-digit numeric OTP/PIN for in-person handover if not already generated
            if (claim.getHandoverPin() == null || claim.getHandoverPin().isBlank()) {
                String pin = String.format("%06d", new SecureRandom().nextInt(1_000_000));
                claim.setHandoverPin(pin);
                claim.setHandoverVerified(false);
            }

            // 1. Mark item as CLAIMED
            item.setStatus("CLAIMED");
            itemRepository.save(item);

            // 2. Reject competing pending claims for this item
            List<Claim> competingClaims = claimRepository.findByItemIdAndIdNot(item.getId(), claim.getId());
            for (Claim other : competingClaims) {
                if ("PENDING".equalsIgnoreCase(other.getStatus())) {
                    other.setStatus("REJECTED");
                    claimRepository.save(other);

                    if (other.getClaimant() != null) {
                        notificationService.sendNotification(
                                other.getClaimant(),
                                "Your claim for '" + item.getTitle() + "' was closed as another claim was approved.",
                                item.getId()
                        );
                    }
                }
            }

            // 3. Notify approved claimant
            if (claim.getClaimant() != null) {
                notificationService.sendNotification(
                        claim.getClaimant(),
                        "Your claim for '" + item.getTitle() + "' has been APPROVED! Use your handover PIN when collecting the item.",
                        item.getId()
                );
            }

            // 4. Broadcast live item status update via WebSocket
            messagingTemplate.convertAndSend(
                    "/topic/items/" + item.getId() + "/status",
                    Map.of("itemId", item.getId(), "status", "CLAIMED")
            );
        } else if ("REJECTED".equals(normalizedStatus) && claim.getClaimant() != null && item != null) {
            notificationService.sendNotification(
                    claim.getClaimant(),
                    "Your claim for '" + item.getTitle() + "' was rejected.",
                    item.getId()
            );
        }

        Claim updated = claimRepository.save(claim);
        return mapToDTO(updated);
    }

    @Override
    @Transactional(noRollbackFor = BadRequestException.class)
    public ClaimResponseDTO verifyHandoverPin(Long itemId, String pin) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", itemId));

        boolean isOwner = item.getUser() != null && item.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = isAdminUser(currentUser);

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("Only the finder who reported this item can verify the handover PIN.");
        }

        Claim approvedClaim = claimRepository.findAll().stream()
                .filter(c -> c.getItem() != null && c.getItem().getId().equals(itemId))
                .filter(c -> "APPROVED".equalsIgnoreCase(c.getStatus()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No approved claim found for this item."));

        if (approvedClaim.getFailedPinAttempts() >= 5) {
            throw new BadRequestException("Verification locked: Maximum failed attempts exceeded. Please contact campus admin.");
        }

        if (pin == null || approvedClaim.getHandoverPin() == null || !approvedClaim.getHandoverPin().equals(pin.trim())) {
            int failed = approvedClaim.getFailedPinAttempts() + 1;
            approvedClaim.setFailedPinAttempts(failed);
            claimRepository.save(approvedClaim);
            int remaining = Math.max(0, 5 - failed);
            throw new BadRequestException("Invalid handover PIN. " + remaining + " attempts remaining before lockout.");
        }

        approvedClaim.setHandoverVerified(true);
        approvedClaim.setFailedPinAttempts(0);
        approvedClaim.setResolvedAt(LocalDateTime.now());
        item.setStatus("REUNITED");
        itemRepository.save(item);
        Claim updated = claimRepository.save(approvedClaim);

        // Broadcast status transition live to all open browsers
        messagingTemplate.convertAndSend(
                "/topic/items/" + item.getId() + "/status",
                Map.of("itemId", item.getId(), "status", "REUNITED")
        );

        if (approvedClaim.getClaimant() != null) {
            notificationService.sendNotification(
                    approvedClaim.getClaimant(),
                    "Handover verified! '" + item.getTitle() + "' has been marked as REUNITED.",
                    item.getId()
            );
        }

        return mapToDTO(updated);
    }

    @Override
    public void deleteClaim(Long id) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "id", id));

        boolean isClaimant = claim.getClaimant() != null && claim.getClaimant().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() != null && "ADMIN".equalsIgnoreCase(currentUser.getRole());

        if (!isClaimant && !isAdmin) {
            throw new UnauthorizedException("You are not authorized to delete this claim.");
        }
        claimRepository.delete(claim);
    }

    @Override
    public ClaimResponseDTO cancelClaim(Long id) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "id", id));

        boolean isClaimant = claim.getClaimant() != null && claim.getClaimant().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() != null && "ADMIN".equalsIgnoreCase(currentUser.getRole());

        if (!isClaimant && !isAdmin) {
            throw new UnauthorizedException("You are not authorized to cancel this claim.");
        }

        if (!"PENDING".equalsIgnoreCase(claim.getStatus())) {
            throw new BadRequestException("Only pending claims can be cancelled.");
        }

        claim.setStatus("CANCELLED");
        return mapToDTO(claimRepository.save(claim));
    }

    @Override
    @Transactional
    public void reopenItem(Long itemId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", itemId));

        boolean isOwner = item.getUser() != null && item.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() != null && "ADMIN".equalsIgnoreCase(currentUser.getRole());

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("Only the author or admin can reopen this listing.");
        }

        String restoredStatus = (item instanceof com.yourteam.lostfound.model.FoundItem
                || item.getClass().getSimpleName().contains("Found")) ? "FOUND" : "LOST";
        item.setStatus(restoredStatus);
        itemRepository.save(item);

        // Cancel approved claim if exists
        claimRepository.findAll().stream()
                .filter(c -> c.getItem() != null && c.getItem().getId().equals(itemId))
                .filter(c -> "APPROVED".equalsIgnoreCase(c.getStatus()))
                .forEach(c -> {
                    c.setStatus("CANCELLED");
                    claimRepository.save(c);
                });

        messagingTemplate.convertAndSend(
                "/topic/items/" + item.getId() + "/status",
                Map.of("itemId", item.getId(), "status", restoredStatus)
        );
    }

    @Override
    public ClaimCommentResponseDTO addComment(Long claimId, ClaimCommentRequestDTO dto) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "id", claimId));

        verifyClaimParticipant(claim, currentUser);

        ClaimComment comment = new ClaimComment(claim, currentUser, dto.getMessage());
        ClaimComment saved = commentRepository.save(comment);

        return mapToCommentDTO(saved);
    }

    @Override
    public List<ClaimCommentResponseDTO> getCommentsByClaimId(Long claimId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "id", claimId));

        verifyClaimParticipant(claim, currentUser);

        return commentRepository.findByClaimIdOrderByCreatedAtAsc(claimId)
                .stream()
                .map(this::mapToCommentDTO)
                .collect(Collectors.toList());
    }

    private void verifyClaimParticipant(Claim claim, User user) {
        boolean isClaimant = claim.getClaimant() != null && claim.getClaimant().getId().equals(user.getId());
        boolean isReporter = claim.getItem() != null && claim.getItem().getUser() != null
                && claim.getItem().getUser().getId().equals(user.getId());
        boolean isAdmin = isAdminUser(user);

        if (!isClaimant && !isReporter && !isAdmin) {
            throw new UnauthorizedException("You are not authorized to view or access this claim.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ClaimAnalyticsDTO getClaimAnalytics() {
        List<Claim> allClaims = claimRepository.findAll();
        long total = allClaims.size();
        if (total == 0) {
            return new ClaimAnalyticsDTO(0, 0, 0, 0, 0.0, 0.0, 0.0);
        }

        long pending = allClaims.stream().filter(c -> "PENDING".equalsIgnoreCase(c.getStatus())).count();
        long approved = allClaims.stream().filter(c -> "APPROVED".equalsIgnoreCase(c.getStatus())).count();
        long rejected = allClaims.stream().filter(c -> "REJECTED".equalsIgnoreCase(c.getStatus())).count();

        double resRate = Math.round(((double) approved / total) * 1000.0) / 10.0;
        double rejRate = Math.round(((double) rejected / total) * 1000.0) / 10.0;

        double avgHours = allClaims.stream()
                .filter(c -> !"PENDING".equalsIgnoreCase(c.getStatus()) && c.getCreatedAt() != null)
                .mapToLong(c -> Duration.between(c.getCreatedAt(), LocalDateTime.now()).toHours())
                .average()
                .orElse(0.0);

        return new ClaimAnalyticsDTO(total, pending, approved, rejected, resRate, rejRate, Math.round(avgHours * 10.0) / 10.0);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportClaimsToCsv() {
        List<Claim> claims = claimRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("Claim ID,Item ID,Item Title,Claimant ID,Claimant Name,Status,Created At,Proof Description\n");

        for (Claim c : claims) {
            String itemTitle = c.getItem() != null ? c.getItem().getTitle().replace(",", " ") : "N/A";
            String claimantName = c.getClaimant() != null ? c.getClaimant().getName().replace(",", " ") : "N/A";
            String proof = c.getProofDescription() != null ? c.getProofDescription().replace("\n", " ").replace(",", ";") : "";

            sb.append(c.getId()).append(",")
                    .append(c.getItem() != null ? c.getItem().getId() : "").append(",")
                    .append("\"").append(itemTitle).append("\",")
                    .append(c.getClaimant() != null ? c.getClaimant().getId() : "").append(",")
                    .append("\"").append(claimantName).append("\",")
                    .append(c.getStatus()).append(",")
                    .append(c.getCreatedAt() != null ? c.getCreatedAt().toString() : "").append(",")
                    .append("\"").append(proof).append("\"\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private ClaimResponseDTO mapToDTO(Claim claim) {
        ClaimResponseDTO dto = new ClaimResponseDTO();
        dto.setId(claim.getId());
        dto.setProofDescription(claim.getProofDescription());
        dto.setVerificationAnswer(claim.getVerificationAnswer());
        // Map stored proof image URL to DTO response
        dto.setProofImageUrl(claim.getProofImageUrl());
        dto.setStatus(claim.getStatus());
        dto.setCreatedAt(claim.getCreatedAt());

        if (claim.getItem() != null) {
            dto.setItemId(claim.getItem().getId());
            dto.setItemTitle(claim.getItem().getTitle());
        }

        if (claim.getClaimant() != null) {
            dto.setClaimantId(claim.getClaimant().getId());
            dto.setClaimantName(claim.getClaimant().getName());
            dto.setClaimantEmail(claim.getClaimant().getEmail());
        }

        User currentUser = null;
        try {
            currentUser = userService.getCurrentAuthenticatedUser();
        } catch (Exception ignored) {}

        boolean isClaimant = currentUser != null && claim.getClaimant() != null
                && currentUser.getId().equals(claim.getClaimant().getId());
        boolean isAdmin = currentUser != null && currentUser.getRole() != null
                && ("ADMIN".equalsIgnoreCase(currentUser.getRole()) || "ROLE_ADMIN".equalsIgnoreCase(currentUser.getRole()));

        String pinValue = claim.getHandoverPin();

        if (isClaimant || isAdmin) {
            dto.setHandoverPin(pinValue);
            dto.setHandoverCode(pinValue);
        } else {
            dto.setHandoverPin(null);
            dto.setHandoverCode(null);
        }
        dto.setHandoverVerified(claim.isHandoverVerified());

        return dto;
    }

    private ClaimCommentResponseDTO mapToCommentDTO(ClaimComment comment) {
        ClaimCommentResponseDTO dto = new ClaimCommentResponseDTO();
        dto.setId(comment.getId());
        dto.setClaimId(comment.getClaim().getId());
        dto.setMessage(comment.getMessage());
        dto.setCreatedAt(comment.getCreatedAt());

        if (comment.getAuthor() != null) {
            dto.setAuthorId(comment.getAuthor().getId());
            dto.setAuthorName(comment.getAuthor().getName());
            dto.setAuthorEmail(comment.getAuthor().getEmail());
        }
        return dto;
    }
}
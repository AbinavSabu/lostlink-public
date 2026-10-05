package com.yourteam.lostfound.service.impl;

import com.yourteam.lostfound.dto.AdminStatsDTO;
import com.yourteam.lostfound.model.Claim;
import com.yourteam.lostfound.model.Item;
import com.yourteam.lostfound.repository.*;
import com.yourteam.lostfound.service.AdminService;
import com.yourteam.lostfound.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {

    private final ItemRepository itemRepository;
    private final ClaimRepository claimRepository;
    private final ClaimCommentRepository claimCommentRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final com.yourteam.lostfound.service.ClaimService claimService;

    public AdminServiceImpl(ItemRepository itemRepository,
                            ClaimRepository claimRepository,
                            ClaimCommentRepository claimCommentRepository,
                            MessageRepository messageRepository,
                            UserRepository userRepository,
                            NotificationRepository notificationRepository,
                            NotificationService notificationService,
                            com.yourteam.lostfound.service.ClaimService claimService) {
        this.itemRepository = itemRepository;
        this.claimRepository = claimRepository;
        this.claimCommentRepository = claimCommentRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.claimService = claimService;
    }

    @Override
    public AdminStatsDTO getDashboardStats() {
        long totalItems = itemRepository.count();
        long lostItems = itemRepository.countLostItems();
        long foundItems = itemRepository.countFoundItems();
        long claimedItems = itemRepository.countByStatusIgnoreCase("CLAIMED");

        long totalClaims = claimRepository.count();
        long pendingClaims = claimRepository.countByStatusIgnoreCase("PENDING");
        long approvedClaims = claimRepository.countByStatusIgnoreCase("APPROVED");

        long totalUsers = userRepository.count();

        return new AdminStatsDTO(
                totalItems,
                lostItems,
                foundItems,
                claimedItems,
                totalClaims,
                pendingClaims,
                approvedClaims,
                totalUsers
        );
    }

    @Override
    @Transactional
    public void deleteItem(Long itemId) {
        claimCommentRepository.deleteByClaimItemId(itemId);
        claimRepository.deleteByItemId(itemId);
        messageRepository.deleteByItemId(itemId);
        itemRepository.deleteById(itemId);
    }

    @Override
    @Transactional
    public void approveClaim(Long claimId) {
        claimService.updateClaimStatus(claimId, "APPROVED");
    }

    @Override
    @Transactional
    public void rejectClaim(Long claimId) {
        claimService.updateClaimStatus(claimId, "REJECTED");
    }

    @Override
    @Transactional
    public void deleteUserSafely(Long userId) {
        if (!userRepository.existsById(userId)) {
            return;
        }

        // 1. Delete notifications received by this user
        notificationRepository.deleteByRecipientId(userId);

        // 2. Delete all comments authored by this user
        claimCommentRepository.deleteByAuthorId(userId);

        // 3. Delete comments attached to claims where this user was the claimant
        claimCommentRepository.deleteByClaimClaimantId(userId);

        // 4. Delete claims filed by this user
        claimRepository.deleteByClaimantId(userId);

        // 5. Delete direct messages sent or received by this user
        messageRepository.deleteByUserId(userId);

        // 6. Delete all items posted by this user (and cascade item claims/messages)
        List<Item> userItems = itemRepository.findByUserId(userId);
        for (Item item : userItems) {
            deleteItem(item.getId());
        }

        // 7. Finally delete the user account
        userRepository.deleteById(userId);
    }
}
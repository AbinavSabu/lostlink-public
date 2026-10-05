package com.yourteam.lostfound.service.impl;

import com.yourteam.lostfound.dto.MessageRequestDTO;
import com.yourteam.lostfound.dto.MessageResponseDTO;
import com.yourteam.lostfound.model.Message;
import com.yourteam.lostfound.repository.MessageRepository;
import com.yourteam.lostfound.repository.UserRepository;
import com.yourteam.lostfound.service.MessageService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.yourteam.lostfound.service.UserService userService;
    private final com.yourteam.lostfound.repository.ItemRepository itemRepository;
    private final com.yourteam.lostfound.repository.ClaimRepository claimRepository;

    public MessageServiceImpl(
            MessageRepository messageRepository,
            UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate,
            com.yourteam.lostfound.service.UserService userService,
            com.yourteam.lostfound.repository.ItemRepository itemRepository,
            com.yourteam.lostfound.repository.ClaimRepository claimRepository
    ) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.userService = userService;
        this.itemRepository = itemRepository;
        this.claimRepository = claimRepository;
    }

    private boolean isAdminUser(com.yourteam.lostfound.model.User user) {
        if (user == null || user.getRole() == null) return false;
        String r = user.getRole().toUpperCase();
        return "ADMIN".equals(r) || "ROLE_ADMIN".equals(r);
    }

    @Override
    public MessageResponseDTO sendMessage(MessageRequestDTO dto) {
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }
        if (dto.getItemId() == null) {
            throw new IllegalArgumentException("Item ID must be provided.");
        }
        if (dto.getRecipientId() == null) {
            throw new IllegalArgumentException("Recipient ID must be provided.");
        }

        com.yourteam.lostfound.model.User currentUser = userService.getCurrentAuthenticatedUser();

        Message message = new Message();
        message.setItemId(dto.getItemId());
        message.setImageUrl(dto.getImageUrl());
        message.setSenderId(currentUser.getId());
        message.setRecipientId(dto.getRecipientId());
        message.setClaimantId(dto.getClaimantId());
        message.setContent(dto.getContent().trim());
        message.setSentAt(LocalDateTime.now());
        message.setRead(false);

        Message saved = messageRepository.save(message);
        MessageResponseDTO responseDTO = mapToDTO(saved);

        // 1. Broadcast to private 1-on-1 scoped thread
        if (saved.getClaimantId() != null) {
            messagingTemplate.convertAndSend(
                    "/topic/items/" + saved.getItemId() + "/thread/" + saved.getClaimantId(),
                    responseDTO
            );
        }

        // 2. Broadcast to general item topic (legacy / fallback)
        messagingTemplate.convertAndSend("/topic/items/" + saved.getItemId(), responseDTO);

        // 3. Dispatch to recipient's personal message stream for navbar badge alerts
        messagingTemplate.convertAndSend("/topic/users/" + saved.getRecipientId() + "/messages", responseDTO);

        return responseDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponseDTO> getThreadByItem(Long itemId) {
        com.yourteam.lostfound.model.User currentUser = userService.getCurrentAuthenticatedUser();
        com.yourteam.lostfound.model.Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new com.yourteam.lostfound.exception.ResourceNotFoundException("Item", "id", itemId));

        boolean isAdmin = isAdminUser(currentUser);
        boolean isOwner = item.getUser() != null && item.getUser().getId().equals(currentUser.getId());
        boolean isClaimant = claimRepository.existsByItemIdAndClaimantId(itemId, currentUser.getId())
                || messageRepository.isParticipantInItemMessages(itemId, currentUser.getId());

        if (!isAdmin && !isOwner && !isClaimant) {
            throw new com.yourteam.lostfound.exception.UnauthorizedException("You are not authorized to view messages for this item.");
        }

        return messageRepository.findByItemIdOrderBySentAtAsc(itemId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponseDTO> getThreadByItemAndClaimant(Long itemId, Long claimantId) {
        com.yourteam.lostfound.model.User currentUser = userService.getCurrentAuthenticatedUser();
        com.yourteam.lostfound.model.Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new com.yourteam.lostfound.exception.ResourceNotFoundException("Item", "id", itemId));

        boolean isAdmin = isAdminUser(currentUser);
        boolean isOwner = item.getUser() != null && item.getUser().getId().equals(currentUser.getId());
        boolean isClaimant = currentUser.getId().equals(claimantId);

        if (!isAdmin && !isOwner && !isClaimant) {
            throw new com.yourteam.lostfound.exception.UnauthorizedException("You are not authorized to view this conversation.");
        }

        return messageRepository.findByItemIdAndClaimantIdOrderBySentAtAsc(itemId, claimantId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getActiveClaimantIds(Long itemId) {
        com.yourteam.lostfound.model.User currentUser = userService.getCurrentAuthenticatedUser();
        com.yourteam.lostfound.model.Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new com.yourteam.lostfound.exception.ResourceNotFoundException("Item", "id", itemId));

        boolean isAdmin = isAdminUser(currentUser);
        boolean isOwner = item.getUser() != null && item.getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new com.yourteam.lostfound.exception.UnauthorizedException("You are not authorized to view claimants for this item.");
        }

        return messageRepository.findDistinctClaimantIdsByItemId(itemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponseDTO> getInbox(Long userId) {
        return messageRepository.findByRecipientIdAndIsReadFalseOrderBySentAtDesc(userId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public void markScopedThreadAsRead(Long itemId, Long claimantId, Long userId) {
        messageRepository.markScopedThreadAsRead(itemId, claimantId, userId);
    }

    @Override
    public void markThreadAsRead(Long itemId, Long userId) {
        messageRepository.markThreadAsRead(itemId, userId);
    }

    private MessageResponseDTO mapToDTO(Message message) {
        MessageResponseDTO dto = new MessageResponseDTO();
        dto.setId(message.getId());
        dto.setItemId(message.getItemId());
        dto.setImageUrl(message.getImageUrl()); // <-- ADD THIS LINE
        dto.setSenderId(message.getSenderId());
        dto.setRecipientId(message.getRecipientId());
        dto.setClaimantId(message.getClaimantId());
        dto.setContent(message.getContent());
        dto.setSentAt(message.getSentAt());
        dto.setRead(message.isRead());

        if (message.getSenderId() != null) {
            userRepository.findById(message.getSenderId())
                    .ifPresent(user -> dto.setSenderName(user.getName()));
        }

        return dto;
    }
}
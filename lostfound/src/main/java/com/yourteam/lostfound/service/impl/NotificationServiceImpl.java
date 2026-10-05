package com.yourteam.lostfound.service.impl;

import com.yourteam.lostfound.dto.NotificationResponseDTO;
import com.yourteam.lostfound.model.Notification;
import com.yourteam.lostfound.model.User;
import com.yourteam.lostfound.repository.NotificationRepository;
import com.yourteam.lostfound.service.NotificationService;
import com.yourteam.lostfound.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            UserService userService,
            org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate
    ) {
        this.notificationRepository = notificationRepository;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void sendNotification(User recipient, String message, Long relatedItemId) {
        Notification notification = new Notification(recipient, message, relatedItemId);
        Notification saved = notificationRepository.save(notification);

        // Dispatch live STOMP event to recipient's notification topic
        try {
            if (recipient != null && recipient.getId() != null) {
                NotificationResponseDTO dto = mapToDTO(saved);
                messagingTemplate.convertAndSend("/topic/users/" + recipient.getId() + "/notifications", dto);
            }
        } catch (Exception ignored) {
            // Avoid failing transaction if WebSocket message fails
        }
    }

    @Override
    public List<NotificationResponseDTO> getMyNotifications() {
        User currentUser = userService.getCurrentAuthenticatedUser();
        if (currentUser == null) {
            return List.of();
        }
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void markAsRead(Long notificationId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        notificationRepository.findById(notificationId).ifPresent(notif -> {
            if (currentUser != null && notif.getRecipient() != null && notif.getRecipient().getId().equals(currentUser.getId())) {
                notif.setRead(true);
                notificationRepository.save(notif);
            }
        });
    }

    @Override
    public void markAllAsRead() {
        User currentUser = userService.getCurrentAuthenticatedUser();
        if (currentUser == null) return;
        List<Notification> unread = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .filter(n -> !n.isRead())
                .toList();
        for (Notification notif : unread) {
            notif.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    private NotificationResponseDTO mapToDTO(Notification n) {
        NotificationResponseDTO dto = new NotificationResponseDTO();
        dto.setId(n.getId());
        dto.setMessage(n.getMessage());
        dto.setRead(n.isRead());
        dto.setRelatedItemId(n.getRelatedItemId());
        dto.setCreatedAt(n.getCreatedAt());
        return dto;
    }
}
package com.yourteam.lostfound.service;

import com.yourteam.lostfound.dto.NotificationResponseDTO;
import com.yourteam.lostfound.model.User;

import java.util.List;

public interface NotificationService {
    void sendNotification(User recipient, String message, Long relatedItemId);
    List<NotificationResponseDTO> getMyNotifications();
    void markAsRead(Long notificationId);
    void markAllAsRead();
}
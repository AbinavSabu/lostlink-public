package com.yourteam.lostfound.service;

import com.yourteam.lostfound.dto.MessageRequestDTO;
import com.yourteam.lostfound.dto.MessageResponseDTO;

import java.util.List;

public interface MessageService {
    MessageResponseDTO sendMessage(MessageRequestDTO dto);
    List<MessageResponseDTO> getThreadByItem(Long itemId);
    List<MessageResponseDTO> getThreadByItemAndClaimant(Long itemId, Long claimantId);
    List<Long> getActiveClaimantIds(Long itemId);
    List<MessageResponseDTO> getInbox(Long userId);
    void markScopedThreadAsRead(Long itemId, Long claimantId, Long userId);
    void markThreadAsRead(Long itemId, Long userId);
}
package com.yourteam.lostfound.controller;

import com.yourteam.lostfound.dto.MessageRequestDTO;
import com.yourteam.lostfound.dto.MessageResponseDTO;
import com.yourteam.lostfound.service.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;
    private final com.yourteam.lostfound.service.UserService userService;
    private final com.yourteam.lostfound.service.FileStorageService fileStorageService;

    public MessageController(
            MessageService messageService,
            com.yourteam.lostfound.service.UserService userService,
            com.yourteam.lostfound.service.FileStorageService fileStorageService
    ) {
        this.messageService = messageService;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping
    public ResponseEntity<MessageResponseDTO> sendMessage(@RequestBody MessageRequestDTO dto) {
        return new ResponseEntity<>(messageService.sendMessage(dto), HttpStatus.CREATED);
    }

    // Legacy/fallback full item thread
    @GetMapping("/item/{itemId}")
    public ResponseEntity<List<MessageResponseDTO>> getItemThread(@PathVariable Long itemId) {
        return ResponseEntity.ok(messageService.getThreadByItem(itemId));
    }

    // Scoped 1-on-1 thread: item + specific claimant/inquirer
    @GetMapping("/item/{itemId}/claimant/{claimantId}")
    public ResponseEntity<List<MessageResponseDTO>> getScopedThread(
            @PathVariable Long itemId,
            @PathVariable Long claimantId
    ) {
        return ResponseEntity.ok(messageService.getThreadByItemAndClaimant(itemId, claimantId));
    }

    // List all distinct claimants who have an active conversation on this item
    @GetMapping("/item/{itemId}/claimants")
    public ResponseEntity<List<Long>> getActiveClaimants(@PathVariable Long itemId) {
        return ResponseEntity.ok(messageService.getActiveClaimantIds(itemId));
    }

    // Inbox messages for recipient (authorized only for self or admin)
    @GetMapping("/inbox/{userId}")
    public ResponseEntity<List<MessageResponseDTO>> getInbox(@PathVariable Long userId) {
        com.yourteam.lostfound.model.User currentUser = userService.getCurrentAuthenticatedUser();
        boolean isAdmin = currentUser.getRole() != null && "ADMIN".equalsIgnoreCase(currentUser.getRole());
        if (!currentUser.getId().equals(userId) && !isAdmin) {
            throw new com.yourteam.lostfound.exception.UnauthorizedException("You are not authorized to view this inbox.");
        }
        return ResponseEntity.ok(messageService.getInbox(userId));
    }

    // Mark scoped thread as read
    @PatchMapping("/item/{itemId}/claimant/{claimantId}/read")
    public ResponseEntity<Void> markScopedThreadAsRead(
            @PathVariable Long itemId,
            @PathVariable Long claimantId,
            @RequestParam Long userId
    ) {
        messageService.markScopedThreadAsRead(itemId, claimantId, userId);
        return ResponseEntity.noContent().build();
    }

    // Legacy mark item thread as read
    @PatchMapping("/item/{itemId}/read")
    public ResponseEntity<Void> markThreadAsRead(@PathVariable Long itemId, @RequestParam Long userId) {
        messageService.markThreadAsRead(itemId, userId);
        return ResponseEntity.noContent().build();
    }
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadChatImage(@RequestParam("file") MultipartFile file) throws IOException {
        String fileUrl = fileStorageService.storeFile(file);
        return ResponseEntity.ok(Map.of("imageUrl", fileUrl));
    }
}
package com.yourteam.lostfound.service.impl;

import com.yourteam.lostfound.dto.ItemRequestDTO;
import com.yourteam.lostfound.dto.ItemResponseDTO;
import com.yourteam.lostfound.exception.ResourceNotFoundException;
import com.yourteam.lostfound.exception.UnauthorizedException;
import com.yourteam.lostfound.model.FoundItem;
import com.yourteam.lostfound.model.Item;
import com.yourteam.lostfound.model.LostItem;
import com.yourteam.lostfound.model.User;
import com.yourteam.lostfound.repository.ItemRepository;
import com.yourteam.lostfound.service.ItemService;
import com.yourteam.lostfound.service.UserService;
import com.yourteam.lostfound.specification.ItemSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserService userService;
    private final com.yourteam.lostfound.service.NotificationService notificationService;
    private final com.yourteam.lostfound.repository.ClaimCommentRepository claimCommentRepository;
    private final com.yourteam.lostfound.repository.ClaimRepository claimRepository;
    private final com.yourteam.lostfound.repository.MessageRepository messageRepository;

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "in", "on", "at", "to", "for", "with",
            "by", "my", "of", "is", "it", "lost", "found", "item", "please", "help",
            "this", "that", "there", "was", "near", "around"
    );

    public ItemServiceImpl(
            ItemRepository itemRepository,
            UserService userService,
            com.yourteam.lostfound.service.NotificationService notificationService,
            com.yourteam.lostfound.repository.ClaimCommentRepository claimCommentRepository,
            com.yourteam.lostfound.repository.ClaimRepository claimRepository,
            com.yourteam.lostfound.repository.MessageRepository messageRepository
    ) {
        this.itemRepository = itemRepository;
        this.userService = userService;
        this.notificationService = notificationService;
        this.claimCommentRepository = claimCommentRepository;
        this.claimRepository = claimRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    public ItemResponseDTO reportItem(ItemRequestDTO dto) {
        User currentUser = userService.getCurrentAuthenticatedUser();

        Item item;
        String status = dto.getStatus() != null ? dto.getStatus().toUpperCase() : "LOST";

        if ("FOUND".equalsIgnoreCase(status)) {
            item = new FoundItem();
        } else {
            item = new LostItem();
        }

        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setCategory(dto.getCategory());
        item.setDate(dto.getDate() != null ? dto.getDate() : LocalDate.now());
        item.setLocation(dto.getLocation());
        item.setStatus(status);
        item.setImageUrl(dto.getImageUrl());
        item.setVerificationQuestion(dto.getVerificationQuestion());
        item.setLatitude(dto.getLatitude());
        item.setLongitude(dto.getLongitude());
        item.setReward(dto.getReward());
        item.setCustodyDesk(dto.getCustodyDesk());
        item.setStorageBin(dto.getStorageBin());
        item.setUser(currentUser);

        Item savedItem = itemRepository.save(item);

        // Automatically evaluate matches and notify relevant item owners
        try {
            triggerAutomatedMatchAlerts(savedItem);
        } catch (Exception ignored) {
            // Avoid failing report transaction if notification fails
        }

        return mapToDTO(savedItem);
    }

    @Override
    public ItemResponseDTO getItemById(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", id));
        return mapToDTO(item);
    }

    @Override
    public List<ItemResponseDTO> getAllItems() {
        return itemRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemResponseDTO> getItemsByStatus(String status) {
        return itemRepository.findAll()
                .stream()
                .filter(item -> status.equalsIgnoreCase(item.getStatus()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemResponseDTO> getItemsByCategory(String category) {
        return itemRepository.findAll()
                .stream()
                .filter(item -> item.getCategory() != null && item.getCategory().equalsIgnoreCase(category))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ItemResponseDTO updateItem(Long id, ItemRequestDTO dto) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", id));

        checkItemOwnershipOrAdmin(item);

        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setCategory(dto.getCategory());
        item.setLocation(dto.getLocation());
        if (dto.getStatus() != null) {
            item.setStatus(dto.getStatus().toUpperCase());
        }
        if (dto.getImageUrl() != null) {
            item.setImageUrl(dto.getImageUrl());
        }
        if (dto.getVerificationQuestion() != null) {
            item.setVerificationQuestion(dto.getVerificationQuestion());
        }
        if (dto.getLatitude() != null) {
            item.setLatitude(dto.getLatitude());
        }
        if (dto.getLongitude() != null) {
            item.setLongitude(dto.getLongitude());
        }
        if (dto.getReward() != null) {
            item.setReward(dto.getReward());
        }
        if (dto.getCustodyDesk() != null) {
            item.setCustodyDesk(dto.getCustodyDesk());
        }
        if (dto.getStorageBin() != null) {
            item.setStorageBin(dto.getStorageBin());
        }

        Item saved = itemRepository.save(item);
        return mapToDTO(saved);
    }

    @Override
    public ItemResponseDTO updateItemStatus(Long id, String status) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", id));

        checkItemOwnershipOrAdmin(item);

        item.setStatus(status.toUpperCase());
        Item updated = itemRepository.save(item);
        return mapToDTO(updated);
    }

    @Override
    public void deleteItem(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", id));

        checkItemOwnershipOrAdmin(item);

        claimCommentRepository.deleteByClaimItemId(id);
        claimRepository.deleteByItemId(id);
        messageRepository.deleteByItemId(id);
        itemRepository.delete(item);
    }

    @Override
    public List<ItemResponseDTO> getMyItems() {
        User currentUser = userService.getCurrentAuthenticatedUser();

        return itemRepository.findByUserId(currentUser.getId())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ItemResponseDTO> getItems(String keyword, String category, String status, Pageable pageable) {
        Specification<Item> spec = ItemSpecification.filterItems(keyword, category, status);
        return itemRepository.findAll(spec, pageable).map(this::mapToDTO);
    }

    @Override
    public List<ItemResponseDTO> getPotentialMatches(Long itemId) {
        Item source = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", itemId));

        String currentStatus = source.getStatus();
        if (currentStatus == null || currentStatus.trim().isEmpty()) {
            currentStatus = (source instanceof FoundItem) ? "FOUND" : "LOST";
        }
        String targetStatus = "LOST".equalsIgnoreCase(currentStatus) ? "FOUND" : "LOST";

        List<Item> candidates = itemRepository.findAll().stream()
                .filter(item -> !item.getId().equals(source.getId()))
                .filter(item -> {
                    String s = item.getStatus();
                    if (s == null) s = (item instanceof FoundItem) ? "FOUND" : "LOST";
                    return targetStatus.equalsIgnoreCase(s);
                })
                .collect(Collectors.toList());

        Set<String> sourceTokens = extractTokens(source.getTitle() + " " + source.getDescription());

        List<Map.Entry<Item, Integer>> scoredCandidates = new ArrayList<>();
        for (Item candidate : candidates) {
            int score = calculateMatchScore(source, candidate, sourceTokens);
            if (score >= 25) {
                scoredCandidates.add(Map.entry(candidate, score));
            }
        }

        return scoredCandidates.stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(entry -> {
                    ItemResponseDTO dto = mapToDTO(entry.getKey());
                    dto.setMatchScore(entry.getValue());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private int calculateMatchScore(Item source, Item candidate, Set<String> sourceTokens) {
        double totalScore = 0.0;

        // 1. Category Matching (up to 30 pts)
        if (source.getCategory() != null && candidate.getCategory() != null) {
            if (source.getCategory().trim().equalsIgnoreCase(candidate.getCategory().trim())) {
                totalScore += 30.0;
            }
        }

        // 2. Token Jaccard + Fuzzy Matching (up to 35 pts)
        Set<String> candidateTokens = extractTokens(candidate.getTitle() + " " + candidate.getDescription());
        if (!sourceTokens.isEmpty() && !candidateTokens.isEmpty()) {
            long exactCount = sourceTokens.stream().filter(candidateTokens::contains).count();
            // Fuzzy similarity check for non-exact words (distance <= 1)
            long fuzzyCount = sourceTokens.stream()
                    .filter(s -> !candidateTokens.contains(s) && candidateTokens.stream().anyMatch(c -> isFuzzyMatch(s, c)))
                    .count();

            double matchWeight = exactCount + (fuzzyCount * 0.75);
            long unionCount = sourceTokens.size() + candidateTokens.size() - exactCount;
            double tokenSimilarity = matchWeight / (unionCount > 0 ? unionCount : 1);
            totalScore += (tokenSimilarity * 35.0);
        }

        // 3. Coordinate Proximity or Location Text (up to 20 pts)
        if (source.getLatitude() != null && source.getLongitude() != null &&
            candidate.getLatitude() != null && candidate.getLongitude() != null) {
            double distanceMeters = haversineDistanceMeters(
                    source.getLatitude(), source.getLongitude(),
                    candidate.getLatitude(), candidate.getLongitude()
            );
            if (distanceMeters <= 250) {
                totalScore += 20.0;
            } else if (distanceMeters <= 1000) {
                totalScore += 15.0;
            } else if (distanceMeters <= 3000) {
                totalScore += 8.0;
            }
        } else if (source.getLocation() != null && candidate.getLocation() != null) {
            String loc1 = source.getLocation().trim().toLowerCase();
            String loc2 = candidate.getLocation().trim().toLowerCase();
            if (!loc1.isEmpty() && !loc2.isEmpty()) {
                if (loc1.equalsIgnoreCase(loc2)) {
                    totalScore += 15.0;
                } else if (loc1.contains(loc2) || loc2.contains(loc1)) {
                    totalScore += 10.0;
                }
            }
        }

        // 4. Date Proximity (up to 15 pts)
        if (source.getDate() != null && candidate.getDate() != null) {
            long daysApart = Math.abs(ChronoUnit.DAYS.between(source.getDate(), candidate.getDate()));
            if (daysApart <= 2) {
                totalScore += 15.0;
            } else if (daysApart <= 7) {
                totalScore += 10.0;
            } else if (daysApart <= 14) {
                totalScore += 5.0;
            }
        }

        return (int) Math.round(Math.min(totalScore, 100.0));
    }

    private boolean isFuzzyMatch(String s1, String s2) {
        if (s1.length() < 4 || s2.length() < 4) return false;
        if (Math.abs(s1.length() - s2.length()) > 1) return false;
        // Simple Levenshtein distance <= 1 check
        int edits = 0, i = 0, j = 0;
        while (i < s1.length() && j < s2.length()) {
            if (s1.charAt(i) != s2.charAt(j)) {
                edits++;
                if (edits > 1) return false;
                if (s1.length() > s2.length()) i++;
                else if (s2.length() > s1.length()) j++;
                else { i++; j++; }
            } else {
                i++; j++;
            }
        }
        return true;
    }

    private double haversineDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private void triggerAutomatedMatchAlerts(Item source) {
        String currentStatus = source.getStatus() != null ? source.getStatus().toUpperCase() : "LOST";
        String targetStatus = "LOST".equals(currentStatus) ? "FOUND" : "LOST";

        Set<String> sourceTokens = extractTokens(source.getTitle() + " " + source.getDescription());

        List<Item> candidates = itemRepository.findAll().stream()
                .filter(item -> !item.getId().equals(source.getId()))
                .filter(item -> targetStatus.equalsIgnoreCase(item.getStatus()))
                .toList();

        for (Item candidate : candidates) {
            int score = calculateMatchScore(source, candidate, sourceTokens);
            if (score >= 60 && candidate.getUser() != null && source.getUser() != null
                    && !candidate.getUser().getId().equals(source.getUser().getId())) {
                String message = String.format(
                        "High potential match (%d%%) detected! A %s item '%s' resembles your reported '%s'.",
                        score,
                        source.getStatus().toLowerCase(),
                        source.getTitle(),
                        candidate.getTitle()
                );
                notificationService.sendNotification(candidate.getUser(), message, source.getId());
            }
        }
    }

    private Set<String> extractTokens(String text) {
        if (text == null || text.isBlank()) return Collections.emptySet();
        return Arrays.stream(text.toLowerCase().split("[^a-zA-Z0-9]+"))
                .filter(word -> word.length() > 2)
                .filter(word -> !STOP_WORDS.contains(word))
                .collect(Collectors.toSet());
    }

    private ItemResponseDTO mapToDTO(Item item) {
        ItemResponseDTO dto = new ItemResponseDTO();
        dto.setId(item.getId());
        dto.setTitle(item.getTitle());
        dto.setDescription(item.getDescription());
        dto.setCategory(item.getCategory());
        dto.setDate(item.getDate());
        dto.setLocation(item.getLocation());
        dto.setStatus(item.getStatus());
        dto.setImageUrl(item.getImageUrl());
        dto.setVerificationQuestion(item.getVerificationQuestion());
        dto.setLatitude(item.getLatitude());
        dto.setLongitude(item.getLongitude());
        dto.setReward(item.getReward());
        dto.setCustodyDesk(item.getCustodyDesk());
        dto.setStorageBin(item.getStorageBin());

        if (item.getUser() != null) {
            dto.setUserId(item.getUser().getId());
            dto.setUserEmail(item.getUser().getEmail());
            dto.setUserName(item.getUser().getName());
        }

        return dto;
    }

    private void checkItemOwnershipOrAdmin(Item item) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            boolean isAdminAuthority = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN") || a.getAuthority().equalsIgnoreCase("ADMIN"));
            if (isAdminAuthority) {
                return; // Admin can modify/status change anything without error
            }
        }

        try {
            User currentUser = userService.getCurrentAuthenticatedUser();
            boolean isAdmin = currentUser.getRole() != null &&
                    (currentUser.getRole().equalsIgnoreCase("ADMIN") || currentUser.getRole().equalsIgnoreCase("ROLE_ADMIN"));
            boolean isOwner = item.getUser() != null && item.getUser().getId().equals(currentUser.getId());

            if (!isAdmin && !isOwner) {
                throw new UnauthorizedException("You are not authorized to modify or delete this item.");
            }
        } catch (Exception e) {
            if (!(e instanceof UnauthorizedException)) {
                throw new UnauthorizedException("You are not authorized to modify or delete this item.");
            }
            throw e;
        }
    }
}
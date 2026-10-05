package com.yourteam.lostfound.controller;

import com.yourteam.lostfound.dto.ItemRequestDTO;
import com.yourteam.lostfound.dto.ItemResponseDTO;
import com.yourteam.lostfound.service.FileStorageService;
import com.yourteam.lostfound.service.ItemService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;
    private final FileStorageService fileStorageService;

    public ItemController(ItemService itemService, FileStorageService fileStorageService) {
        this.itemService = itemService;
        this.fileStorageService = fileStorageService;
    }

    // 1. File upload
    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String fileUrl = fileStorageService.storeFile(file);
            return ResponseEntity.ok(Map.of("imageUrl", fileUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 2. Creation
    @PostMapping
    public ResponseEntity<ItemResponseDTO> reportItem(@RequestBody ItemRequestDTO requestDTO) {
        ItemResponseDTO response = itemService.reportItem(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 3. Unified Search & Paginated Feed
    @GetMapping
    public ResponseEntity<Page<ItemResponseDTO>> getItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "date", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(itemService.getItems(keyword, category, status, pageable));
    }

    // 4. Static sub-paths MUST be declared before /{id}
    @GetMapping("/my-items")
    public ResponseEntity<List<ItemResponseDTO>> getMyItems() {
        return ResponseEntity.ok(itemService.getMyItems());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ItemResponseDTO>> getItemsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(itemService.getItemsByStatus(status));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<ItemResponseDTO>> getItemsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(itemService.getItemsByCategory(category));
    }

    // 5. Item match suggestions (declared BEFORE /{id})
    @GetMapping("/{id}/matches")
    public ResponseEntity<List<ItemResponseDTO>> getPotentialMatches(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.getPotentialMatches(id));
    }

    // 6. Dynamic ID routes
    @GetMapping("/{id}")
    public ResponseEntity<ItemResponseDTO> getItemById(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.getItemById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemResponseDTO> updateItem(@PathVariable Long id, @RequestBody ItemRequestDTO dto) {
        return ResponseEntity.ok(itemService.updateItem(id, dto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ItemResponseDTO> updateItemStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(itemService.updateItemStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
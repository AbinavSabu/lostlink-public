package com.yourteam.lostfound.service;

import com.yourteam.lostfound.dto.ItemRequestDTO;
import com.yourteam.lostfound.dto.ItemResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ItemService {
    ItemResponseDTO reportItem(ItemRequestDTO dto);
    ItemResponseDTO getItemById(Long id);
    List<ItemResponseDTO> getAllItems();
    List<ItemResponseDTO> getItemsByStatus(String status);
    List<ItemResponseDTO> getItemsByCategory(String category);
    ItemResponseDTO updateItem(Long id, ItemRequestDTO dto);
    ItemResponseDTO updateItemStatus(Long id, String status);
    void deleteItem(Long id);
    List<ItemResponseDTO> getMyItems();
    Page<ItemResponseDTO> getItems(String keyword, String category, String status, Pageable pageable);

    // Add this declaration:
    List<ItemResponseDTO> getPotentialMatches(Long itemId);
}
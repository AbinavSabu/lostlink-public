package com.yourteam.lostfound.repository;

import com.yourteam.lostfound.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    List<Item> findByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Item i WHERE i.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(i) FROM Item i WHERE TYPE(i) = com.yourteam.lostfound.model.LostItem")
    long countLostItems();

    @Query("SELECT COUNT(i) FROM Item i WHERE TYPE(i) = com.yourteam.lostfound.model.FoundItem")
    long countFoundItems();

    @Query("SELECT COUNT(i) FROM Item i WHERE UPPER(i.status) = UPPER(:status)")
    long countByStatusIgnoreCase(@Param("status") String status);

    List<Item> findByImageUrlStartingWith(String prefix);

    @Modifying
    @Transactional
    @Query("UPDATE Item i SET i.imageUrl = :newUrl WHERE i.id = :id")
    void updateImageUrl(@Param("id") Long id, @Param("newUrl") String newUrl);

    @Query("SELECT i FROM Item i WHERE " +
            "i.id <> :itemId AND " +
            "(UPPER(i.status) = UPPER(:targetStatus) OR " +
            " (UPPER(:targetStatus) = 'FOUND' AND TYPE(i) = com.yourteam.lostfound.model.FoundItem) OR " +
            " (UPPER(:targetStatus) = 'LOST' AND TYPE(i) = com.yourteam.lostfound.model.LostItem)) AND " +
            "(LOWER(i.category) = LOWER(:category) OR " +
            " (:keyword <> '' AND (LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(i.description) LIKE LOWER(CONCAT('%', :keyword, '%')))))")
    List<Item> findPotentialMatches(
            @Param("itemId") Long itemId,
            @Param("targetStatus") String targetStatus,
            @Param("category") String category,
            @Param("keyword") String keyword
    );
}
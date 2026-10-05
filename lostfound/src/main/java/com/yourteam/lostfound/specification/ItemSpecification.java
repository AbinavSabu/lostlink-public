package com.yourteam.lostfound.specification;

import com.yourteam.lostfound.model.Item;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class ItemSpecification {

    public static Specification<Item> filterItems(String keyword, String category, String status) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                Predicate titleLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern);
                Predicate descLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern);
                Predicate locLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("location")), pattern);
                predicates.add(criteriaBuilder.or(titleLike, descLike, locLike));
            }

            if (StringUtils.hasText(category)) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("category")), category.toLowerCase()));
            }

            if (StringUtils.hasText(status)) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.upper(root.get("status")), status.toUpperCase()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
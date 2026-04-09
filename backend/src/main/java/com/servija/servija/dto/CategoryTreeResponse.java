package com.servija.servija.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTreeResponse {
    private List<CategoryNode> categories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryNode {
        private Long id;
        private String slug;
        private String name;
        private Integer sortOrder;
        private Boolean active;
        private List<SubcategoryNode> subcategories;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubcategoryNode {
        private Long id;
        private Long categoryId;
        private String slug;
        private String name;
        private Integer sortOrder;
        private Boolean active;
    }
}

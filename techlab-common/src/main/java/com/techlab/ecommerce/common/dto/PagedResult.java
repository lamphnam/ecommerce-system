package com.techlab.ecommerce.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagedResult {
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
}

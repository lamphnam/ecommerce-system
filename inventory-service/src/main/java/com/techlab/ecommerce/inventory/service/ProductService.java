package com.techlab.ecommerce.inventory.service;

import com.techlab.ecommerce.inventory.dto.request.AdjustStockRequest;
import com.techlab.ecommerce.inventory.dto.request.CreateProductRequest;
import com.techlab.ecommerce.inventory.dto.response.ProductResponse;

public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse getProduct(Long id);

    ProductResponse adjustStock(Long id, AdjustStockRequest request);
}

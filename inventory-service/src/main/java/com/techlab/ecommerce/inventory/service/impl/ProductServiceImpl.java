package com.techlab.ecommerce.inventory.service.impl;

import com.techlab.ecommerce.inventory.dto.request.AdjustStockRequest;
import com.techlab.ecommerce.inventory.dto.request.CreateProductRequest;
import com.techlab.ecommerce.inventory.dto.response.ProductResponse;
import com.techlab.ecommerce.inventory.entity.Product;
import com.techlab.ecommerce.inventory.enums.InventoryErrorMessage;
import com.techlab.ecommerce.inventory.exception.InventoryException;
import com.techlab.ecommerce.inventory.mapper.ProductMapper;
import com.techlab.ecommerce.inventory.repository.ProductRepository;
import com.techlab.ecommerce.inventory.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        String sku = normalizeSku(request.getSku());
        if (productRepository.existsBySku(sku)) {
            throw new InventoryException(InventoryErrorMessage.PRODUCT_SKU_ALREADY_EXISTS,
                    "SKU already exists: " + sku);
        }

        Product product = Product.builder()
                .sku(sku)
                .name(request.getName().trim())
                .price(request.getPrice())
                .stock(request.getStock())
                .active(request.getActive() == null || request.getActive())
                .build();

        try {
            Product saved = productRepository.saveAndFlush(product);
            log.info("Created product {} sku={} stock={}", saved.getId(), saved.getSku(), saved.getStock());
            return productMapper.toDto(saved);
        } catch (DataIntegrityViolationException duplicateSkuRace) {
            throw new InventoryException(InventoryErrorMessage.PRODUCT_SKU_ALREADY_EXISTS,
                    "SKU already exists: " + sku);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        return productMapper.toDto(loadProduct(id));
    }

    @Override
    @Transactional
    public ProductResponse adjustStock(Long id, AdjustStockRequest request) {
        Product product = loadProduct(id);
        product.setStock(request.getStock());
        Product saved = productRepository.saveAndFlush(product);
        log.info("Adjusted product {} stock to {}", id, request.getStock());
        return productMapper.toDto(saved);
    }

    private Product loadProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new InventoryException(InventoryErrorMessage.PRODUCT_NOT_FOUND,
                        "Product " + id + " not found"));
    }

    private static String normalizeSku(String sku) {
        return sku == null ? null : sku.trim().toUpperCase();
    }
}

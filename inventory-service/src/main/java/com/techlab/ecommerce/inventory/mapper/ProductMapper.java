package com.techlab.ecommerce.inventory.mapper;

import com.techlab.ecommerce.common.mapper.SuperConverter;
import com.techlab.ecommerce.inventory.dto.response.ProductResponse;
import com.techlab.ecommerce.inventory.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper extends SuperConverter<ProductResponse, Product> {

    @Override
    public ProductResponse toDto(Product entity) {
        if (entity == null) {
            return null;
        }
        return ProductResponse.builder()
                .id(entity.getId())
                .sku(entity.getSku())
                .name(entity.getName())
                .price(entity.getPrice())
                .stock(entity.getStock())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    public Product toEntity(ProductResponse dto) {
        throw new UnsupportedOperationException(
                "ProductResponse → Product is not supported; products are built from CreateProductRequest");
    }
}

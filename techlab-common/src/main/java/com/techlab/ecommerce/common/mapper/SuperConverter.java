package com.techlab.ecommerce.common.mapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic two-way converter base. Implementations call into {@link org.modelmapper.ModelMapper}
 * (or write manual mapping) to translate between an entity {@code E} and its DTO {@code D}.
 */
public abstract class SuperConverter<D, E> {

    public List<D> toDtoList(List<E> entities) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }
        List<D> result = new ArrayList<>(entities.size());
        for (E entity : entities) {
            result.add(toDto(entity));
        }
        return result;
    }

    public List<E> toEntityList(List<D> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return new ArrayList<>();
        }
        List<E> result = new ArrayList<>(dtos.size());
        for (D dto : dtos) {
            result.add(toEntity(dto));
        }
        return result;
    }

    public abstract D toDto(E entity);

    public abstract E toEntity(D dto);
}

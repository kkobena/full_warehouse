package com.kobe.warehouse.repository;

import org.springframework.data.jpa.domain.Specification;

public interface SpecificationBuilder {
    default <T> Specification<T> add(Specification<T> base, Specification<T> next) {
        if (base == null) {
            return next;
        }
        if (next == null) {
            return base;
        }
        return base.and(next);
    }
}

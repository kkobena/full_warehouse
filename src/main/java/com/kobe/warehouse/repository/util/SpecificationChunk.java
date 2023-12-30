package com.kobe.warehouse.repository.util;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class SpecificationChunk<T> implements Specification<T> {
    private final Condition condition;

    public SpecificationChunk(Condition condition) {
        this.condition = condition;
    }

    private Path<Number> buildExpression(Root<T> root, String[] leftHand) {

        switch (leftHand.length) {
            case 1:
                return root.get(leftHand[0]);
            case 2:
                return root.get(leftHand[0]).get(leftHand[1]);
            case 3:
                return root.get(leftHand[0]).get(leftHand[1]).get(leftHand[2]);
            case 4:
                return root.get(leftHand[0]).get(leftHand[1]).get(leftHand[2]).get(leftHand[3]);
            default:
                return null;
        }


    }

    private Path<String> build(Root<T> root, String[] leftHand) {

        switch (leftHand.length) {
            case 1:
                return root.get(leftHand[0]);
            case 2:
                return root.get(leftHand[0]).get(leftHand[1]);
            case 3:
                return root.get(leftHand[0]).get(leftHand[1]).get(leftHand[2]);
            case 4:
                return root.get(leftHand[0]).get(leftHand[1]).get(leftHand[2]).get(leftHand[3]);
            default:
                return null;
        }


    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb) {
        switch (condition.getOperation()) {
            case EQUAL:
                return cb.equal(build(root, condition.getLeftHand()),
                    condition.getRightHand());
            case NOT_EQUAL:
                return cb.notEqual(build(root, condition.getLeftHand()),
                    condition.getRightHand());
            case GREATER_THAN:
                return cb.greaterThan(build(root, condition.getLeftHand()),
                    condition.getRightHand());
            case LESS_THAN:
                return cb.lessThan(build(root, condition.getLeftHand()),
                    condition.getRightHand());
            case LIKE:
                return cb.like(cb.upper(build(root, condition.getLeftHand())),
                    condition.getRightHand().toUpperCase());
            case LESS_OR_EQUAL_THAN:
                return cb.lessThanOrEqualTo(build(root, condition.getLeftHand()),
                    condition.getRightHand());
            case GREATER_OR_EQUAL_THAN:
                return cb.greaterThanOrEqualTo(build(root, condition.getLeftHand()),
                    condition.getRightHand());
            default:
                return null;
        }

    }
}

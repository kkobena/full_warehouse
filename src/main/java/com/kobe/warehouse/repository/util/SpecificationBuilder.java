package com.kobe.warehouse.repository.util;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class SpecificationBuilder<T> {
    private final List<Condition> conditions;
    public SpecificationBuilder() {
        conditions = new ArrayList<>();
    }
    public SpecificationBuilder<T> with(String[] leftHand, String rightHand,
                                        Condition.OperationType operation, Condition.LogicalOperatorType operator) {
        conditions.add(new Condition(leftHand, rightHand,
            operation, operator));
        return this;
    }
    public Specification<T> build() {
        if (conditions.isEmpty()) {
            return null;
        }
        List<Specification<T>> specifications = new ArrayList<>();
        for (Condition condition : conditions) {
            specifications.add(new SpecificationChunk(condition));
        }
        Specification<T> finalSpecification = specifications.get(0);
        for (int i = 1; i < conditions.size(); i++) {
            if (!conditions.get(i - 1).getOperator()
                .equals(Condition.LogicalOperatorType.END)) {
                finalSpecification = conditions.get(i - 1).getOperator()
                    .equals(Condition.LogicalOperatorType.OR)
                    ? Specification.where(finalSpecification)
                    .or(specifications.get(i))
                    : Specification.where(finalSpecification)
                    .and(specifications.get(i));
            }
        }
        return finalSpecification;
    }
}

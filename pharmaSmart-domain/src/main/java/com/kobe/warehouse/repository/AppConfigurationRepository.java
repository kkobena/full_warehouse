package com.kobe.warehouse.repository;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.domain.AppConfiguration_;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public interface AppConfigurationRepository extends JpaRepository<AppConfiguration, String>, JpaSpecificationExecutor<AppConfiguration> {

    static Specification<AppConfiguration> buildSpec(String search) {
        Specification<AppConfiguration> spec = Specification.unrestricted();
        if (StringUtils.hasText(search)) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, _, cb) ->
                cb.or(
                    cb.like(cb.lower(root.get(AppConfiguration_.name)), pattern),
                    cb.like(cb.lower(root.get(AppConfiguration_.description)), pattern)
                )
            );
        }
        spec= spec.and((root, _, cb) -> cb.not(root.get(AppConfiguration_.name).in(EntityConstant.APP_PONCTION_ANNULATION_MAX_DAYS,EntityConstant.EXCLUDE_FREE_UNIT)));
        return spec;
    }
}

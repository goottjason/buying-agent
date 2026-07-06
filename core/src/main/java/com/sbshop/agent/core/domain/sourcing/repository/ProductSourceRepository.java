package com.sbshop.agent.core.domain.sourcing.repository;

import com.sbshop.agent.core.domain.sourcing.model.ProductSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import com.sbshop.agent.core.domain.common.enums.EntityStatus;

@Repository
public interface ProductSourceRepository extends JpaRepository<ProductSource, Long> {
    List<ProductSource> findByStatusOrderByIdDesc(EntityStatus status);
}

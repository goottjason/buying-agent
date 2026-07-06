package com.sbshop.agent.core.domain.sourcing.repository;

import com.sbshop.agent.core.domain.sourcing.model.SourcingSite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SourcingSiteRepository extends JpaRepository<SourcingSite, Long> {
}

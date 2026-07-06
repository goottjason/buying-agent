package com.sbshop.agent.core.domain.sourcing.repository;

import com.sbshop.agent.core.domain.sourcing.model.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {
}

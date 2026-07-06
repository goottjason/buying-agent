package com.sbshop.agent.api.sourcing.controller;

import com.sbshop.agent.api.common.response.CommonResponse;
import com.sbshop.agent.core.domain.sourcing.model.SyncLog;
import com.sbshop.agent.core.domain.sourcing.repository.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/api/v1/sync-logs")
@RequiredArgsConstructor
public class SyncLogController {

    private final SyncLogRepository repository;

    @GetMapping
    public CommonResponse<List<SyncLogResponse>> getLogs() {
        List<SyncLog> logs = repository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        List<SyncLogResponse> responses = logs.stream().map(log -> new SyncLogResponse(
            log.getId().toString(),
            log.getCreatedAt() != null ? log.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "-",
            log.getSiteCode(),
            log.getProductName(),
            log.getSyncType(),
            log.getSyncStatus(),
            log.getMessage()
        )).collect(Collectors.toList());

        return CommonResponse.ok(responses);
    }

    public record SyncLogResponse(
        String key,
        String time,
        String site,
        String product,
        String type,
        String status,
        String msg
    ) {}
}

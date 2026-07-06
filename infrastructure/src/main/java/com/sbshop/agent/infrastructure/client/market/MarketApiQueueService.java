package com.sbshop.agent.infrastructure.client.market;

import com.sbshop.agent.core.domain.sourcing.model.enums.StockStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketApiQueueService {

    private final MarketApiAdapter marketApiAdapter;
    
    // 임시 인메모리 큐 (실제 운영 시에는 RabbitMQ, Kafka, Redis Queue 등 사용)
    private final BlockingQueue<MarketSyncTask> queue = new LinkedBlockingQueue<>(10000);

    /**
     * 마켓 동기화 작업을 큐에 추가합니다.
     */
    public void enqueueSyncTask(String marketProductId, BigDecimal targetPrice, StockStatus stockStatus) {
        boolean added = queue.offer(new MarketSyncTask(marketProductId, targetPrice, stockStatus));
        if (added) {
            log.debug("마켓 동기화 작업 큐 추가 완료 (총 대기건수: {})", queue.size());
        } else {
            log.warn("마켓 동기화 큐가 가득 찼습니다. 유실 방지 대책 필요!");
        }
    }

    /**
     * 별도의 쓰레드에서 큐를 소비하며 마켓 API를 호출합니다. (Rate Limit 방어용)
     * 초당 N회 이하로 호출하도록 제어.
     */
    @Async
    public void processQueue() {
        while (true) {
            try {
                MarketSyncTask task = queue.take(); // 큐가 비어있으면 블락킹
                
                marketApiAdapter.updateMarketProduct(task.marketProductId(), task.targetPrice(), task.stockStatus());
                
                // 쿠팡 API Rate Limit (예: 초당 2건) 방어를 위한 딜레이
                Thread.sleep(500); 
                
            } catch (InterruptedException e) {
                log.warn("큐 프로세서 쓰레드 인터럽트 발생", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("마켓 API 호출 중 에러 발생", e);
            }
        }
    }

    // 큐에 담을 Task DTO
    record MarketSyncTask(String marketProductId, BigDecimal targetPrice, StockStatus stockStatus) {}
}

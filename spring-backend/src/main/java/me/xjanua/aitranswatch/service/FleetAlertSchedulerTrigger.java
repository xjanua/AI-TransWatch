package me.xjanua.aitranswatch.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Bộ kích hoạt lịch cho {@link FleetAlertScheduler}.
 *
 * <p>Tách riêng khỏi service nghiệp vụ để lời gọi đi qua proxy Spring, nhờ đó
 * {@code @Transactional} trên {@code runChecks()} vẫn được áp dụng (self-invocation sẽ bỏ qua proxy).
 * Endpoint demo cũng gọi lại chính service này để kiểm thử thủ công.
 */
@Component
public class FleetAlertSchedulerTrigger {
    private final FleetAlertScheduler scheduler;

    public FleetAlertSchedulerTrigger(FleetAlertScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Scheduled(fixedDelayString = "${alerts.check-interval-ms:60000}", initialDelayString = "${alerts.check-initial-delay-ms:30000}")
    public void trigger() {
        scheduler.runChecks();
    }
}

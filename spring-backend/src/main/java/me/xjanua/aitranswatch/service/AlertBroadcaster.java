package me.xjanua.aitranswatch.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import me.xjanua.aitranswatch.dto.alert.AlertCreatedEvent;

/**
 * Phát cảnh báo mới tới các dashboard đang mở qua STOMP.
 *
 * <p>Chỉ phát sau khi giao dịch đã commit, nhờ vậy client không bao giờ nhận được cảnh báo
 * của một giao dịch bị rollback. Việc phát tin tách rời khỏi nghiệp vụ: nếu không có client
 * nào đang kết nối thì Spring bỏ qua an toàn, cảnh báo vẫn được lưu trong cơ sở dữ liệu.
 */
@Service
public class AlertBroadcaster {
    public static final String ALERTS_TOPIC = "/topic/alerts";

    private final SimpMessagingTemplate messagingTemplate;

    public AlertBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAlertCreated(AlertCreatedEvent event) {
        messagingTemplate.convertAndSend(ALERTS_TOPIC, event.alert());
    }
}

package me.xjanua.aitranswatch.dto.alert;

/** Sự kiện nội bộ phát ra sau khi một cảnh báo được lưu thành công. */
public record AlertCreatedEvent(AlertResponse alert) {
}

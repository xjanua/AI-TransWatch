package me.xjanua.aitranswatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Ứng dụng backend AI-TransWatch; bật lịch tác vụ cho các kiểm tra cảnh báo định kỳ. */
@SpringBootApplication
@EnableScheduling
public class AiTransWatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiTransWatchApplication.class, args);
    }
}

package backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5); // Cấp 5 luồng xử lý song song đề phòng nhiều học sinh trùng giờ reo chuông
        scheduler.setThreadNamePrefix("StudyReminder-");
        scheduler.initialize();
        return scheduler;
    }
}
package media.toloka.rfa.media.messenger.config;

//public class StompHeartbeatSheduler {
//}

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;

@EnableScheduling
@EnableAsync
@Configuration
public class StompHeartbeatSheduler implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("StompHeartbeatSheduler");
        scheduler.initialize();
        return scheduler;
    }

//    public StompHeartbeatSheduler()

}
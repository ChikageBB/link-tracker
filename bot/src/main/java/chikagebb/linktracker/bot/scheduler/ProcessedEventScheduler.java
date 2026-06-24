package chikagebb.linktracker.bot.scheduler;

import chikagebb.linktracker.bot.repository.ProcessedEventRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessedEventScheduler {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanUp() {
        int deleted =
                processedEventRepository.deleteOlderThan(OffsetDateTime.now().minusDays(7));
        log.info("cleaned {} old processed events", deleted);
    }
}

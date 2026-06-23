package chikagebb.linktracker.scrapper.scheduler;

import chikagebb.linktracker.scrapper.checker.LinkChecker;
import chikagebb.linktracker.scrapper.client.NotificationClient;
import chikagebb.linktracker.scrapper.dto.LinkDto;
import chikagebb.linktracker.scrapper.properties.ScrapperProperties;
import chikagebb.linktracker.scrapper.repository.LinkRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkUpdateScheduler {

    private final LinkRepository linkRepository;
    private final NotificationClient notificationClient;
    private final ScrapperProperties props;
    private final ExecutorService executorService;
    private final List<LinkChecker> linkCheckers;

    @Scheduled(fixedDelayString = "${app.scheduler.interval}")
    public void checkUpdates() {
        int page = 0;
        int size = props.getBatchSize();

        Map<Long, List<LinkDto>> batch;

        do {
            batch = linkRepository.findAllLinkSubscriber(page, size);
            processBatch(batch);
            page++;
        } while (!batch.isEmpty());
    }

    private void processBatch(Map<Long, List<LinkDto>> batch) {

        List<Map.Entry<Long, LinkDto>> allTasks = new ArrayList<>();
        for (var entry : batch.entrySet()) {
            Long chatId = entry.getKey();
            for (LinkDto link : entry.getValue()) {
                allTasks.add(Map.entry(chatId, link));
            }
        }

        Map<Long, List<String>> failedByChatId = new ConcurrentHashMap<>();
        List<Future<?>> futures = new ArrayList<>();

        int threadCount = props.getThreadCount();
        int chunkSize = (int) Math.ceil((double) allTasks.size() / threadCount);

        for (int i = 0; i < allTasks.size(); i += chunkSize) {
            List<Map.Entry<Long, LinkDto>> chunk = allTasks.subList(i, Math.min(i + chunkSize, allTasks.size()));

            futures.add(executorService.submit(() -> {
                for (Map.Entry<Long, LinkDto> task : chunk) {
                    Long chatId = task.getKey();
                    LinkDto link = task.getValue();

                    try {
                        checkLink(chatId, link);
                    } catch (Exception e) {
                        log.error("Ошибка обработки ссылки {}: {}", link.getUrl(), e.getMessage());
                        failedByChatId
                                .computeIfAbsent(chatId, k -> new CopyOnWriteArrayList<>())
                                .add(link.getUrl().toString());
                    }
                }
            }));
        }

        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException e) {
                log.warn("Ожидание результатов батча было прервано", e);
                Thread.currentThread().interrupt();
                futures.forEach(future -> future.cancel(true));
                break;
            } catch (Exception e) {
                log.error("Ошибка получения результата: {}", e.getMessage());
            }
        }

        if (!failedByChatId.isEmpty()) {
            failedByChatId.forEach((chatId, urls) -> {
                String report = "⚠ Не удалось проверить ссылки:\n" + String.join("\n", urls);
                notificationClient.sendUpdate(null, "", report, List.of(chatId));
            });
        }
    }

    private void checkLink(Long chatId, LinkDto link) {
        log.info("Проверяем ссылку: {}, lastCheckAt: {}", link.getUrl(), link.getLastCheckAt());

        linkCheckers.stream()
                .filter(linkChecker -> linkChecker.support(link.getUrl()))
                .findFirst()
                .ifPresentOrElse(
                        linkChecker -> linkChecker.check(link).ifPresent(message -> {
                            notificationClient.sendUpdate(
                                    link.getId(), link.getUrl().toString(), message, List.of(chatId));
                            linkRepository.updateLastCheckAt(link.getId(), OffsetDateTime.now());
                        }),
                        () -> log.debug("Неизвестный тип ссылки: {}", link.getUrl()));
    }
}

package chikagebb.linktracker.scrapper.service;

import chikagebb.linktracker.scrapper.properties.OutboxProperties;
import chikagebb.linktracker.scrapper.repository.orm.OutboxEventRepository;
import chikagebb.linktracker.scrapper.repository.orm.entity.OutboxEvent;
import chikagebb.linktracker.scrapper.repository.orm.entity.OutboxStatus;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class OutboxTransactionalService {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxProperties props;

    public void processBatch(int limit, Function<OutboxEvent, CompletableFuture<?>> sender) {
        var events = outboxEventRepository.findUnpublishedForUpdate(props.getMaxAttempts(), limit);
        if (events.isEmpty()) return;

        var futures = events.stream().map(sender).toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .orTimeout(30, TimeUnit.SECONDS)
                .exceptionally(ex -> null)
                .join();

        for (int i = 0; i < events.size(); i++) {
            var e = events.get(i);
            var f = futures.get(i);

            if (f.isDone() && !f.isCompletedExceptionally()) {
                markPublishedInline(e);
            } else {
                String err = f.isCompletedExceptionally() ? getCauseMessage(f) : "send timeout";
                markFailedInline(e, err);
                f.cancel(true);
            }
        }
    }

    private void markPublishedInline(OutboxEvent event) {
        event.setPublishedAt(OffsetDateTime.now());
        outboxEventRepository.save(event);
    }

    private void markFailedInline(OutboxEvent event, String errorMessage) {
        int newAttempts = event.getAttempts() + 1;
        event.setAttempts(newAttempts);
        event.setLastError(errorMessage);

        if (newAttempts >= props.getMaxAttempts()) {
            event.setStatus(OutboxStatus.FAILED);
        }

        outboxEventRepository.save(event);
    }

    private String getCauseMessage(CompletableFuture<?> future) {
        try {
            future.join();
            return null;
        } catch (Exception e) {
            Throwable cause = e.getCause();
            return cause != null ? cause.getMessage() : e.getMessage();
        }
    }
}

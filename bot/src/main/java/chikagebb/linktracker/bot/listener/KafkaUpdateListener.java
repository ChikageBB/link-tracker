package chikagebb.linktracker.bot.listener;

import chikagebb.linktracker.avro.LinkUpdateEvent;
import chikagebb.linktracker.bot.service.IdempotencyService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaUpdateListener {

    private final TelegramBot telegramBot;
    private final IdempotencyService idempotencyService;

    @RetryableTopic(
            attempts = "${app.kafka.retry.max-attempts}",
            backOff = @BackOff(delay = 1000, multiplier = 2),
            dltTopicSuffix = ".dlt",
            exclude = {
                IllegalArgumentException.class,
                org.springframework.kafka.support.serializer.DeserializationException.class
            },
            autoCreateTopics = "false")
    @KafkaListener(
            topics = "${app.kafka.topic.name}",
            groupId = "${app.kafka.topic.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    @Transactional(rollbackFor = Exception.class)
    public void onUpdate(
            @Payload LinkUpdateEvent event, @Header(name = "event-id", required = false) String eventIdHeader) {

        if (eventIdHeader == null) {
            throw new IllegalArgumentException("Missing event-id header");
        }

        UUID eventId = UUID.fromString(eventIdHeader);
        if (!idempotencyService.tryMarkProcessed(eventId)) {
            log.atDebug()
                    .setMessage("duplicate event ignored")
                    .addKeyValue("eventId", eventId)
                    .log();
            return;
        }

        if (event.getUrl() == null) {
            throw new IllegalArgumentException("Invalid event: " + event);
        }

        telegramBot.execute(
                new SendMessage(event.getTgChatId(), event.getDescription().toString()));
    }

    @DltHandler
    public void handleDlq(LinkUpdateEvent event) {
        log.atError()
                .setMessage("Message moved to DLT")
                .addKeyValue("event", event)
                .log();
    }
}

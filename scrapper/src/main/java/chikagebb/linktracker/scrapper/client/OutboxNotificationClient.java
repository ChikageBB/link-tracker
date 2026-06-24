package chikagebb.linktracker.scrapper.client;

import chikagebb.linktracker.scrapper.dto.LinkUpdateEvent;
import chikagebb.linktracker.scrapper.properties.KafkaTopicProperties;
import chikagebb.linktracker.scrapper.repository.orm.OutboxEventRepository;
import chikagebb.linktracker.scrapper.repository.orm.entity.OutboxEvent;
import chikagebb.linktracker.scrapper.repository.orm.entity.OutboxStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
public class OutboxNotificationClient implements NotificationClient {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTopicProperties props;

    @Override
    @Transactional
    public void sendUpdate(Long id, String url, String description, List<Long> chatIds) {
        for (Long chatId : chatIds) {
            var event = new LinkUpdateEvent(id, url, description, chatId);

            var outbox = new OutboxEvent();
            outbox.setEventId(UUID.randomUUID());
            outbox.setAggregateId(id);
            outbox.setTopic(props.getTopic().getName());
            outbox.setPayload(objectMapper.writeValueAsString(event));
            outbox.setCreatedAt(OffsetDateTime.now());
            outbox.setStatus(OutboxStatus.PENDING);

            outboxEventRepository.save(outbox);
        }
    }
}

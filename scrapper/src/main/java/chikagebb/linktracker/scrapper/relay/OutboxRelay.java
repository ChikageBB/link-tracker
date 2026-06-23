package chikagebb.linktracker.scrapper.relay;

import chikagebb.linktracker.scrapper.dto.LinkUpdateEvent;
import chikagebb.linktracker.scrapper.repository.orm.entity.OutboxEvent;
import chikagebb.linktracker.scrapper.service.OutboxTransactionalService;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxTransactionalService txService;
    private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.outbox.interval}")
    public void publish() {
        txService.processBatch(100, this::sendToKafkaAsync);
    }

    private CompletableFuture<?> sendToKafkaAsync(OutboxEvent event) {
        try {
            var pojo = objectMapper.readValue(event.getPayload(), LinkUpdateEvent.class);

            var avro = chikagebb.linktracker.avro.LinkUpdateEvent.newBuilder()
                    .setId(pojo.id())
                    .setUrl(pojo.url())
                    .setDescription(pojo.description())
                    .setTgChatId(pojo.tgChatId())
                    .build();

            var record =
                    new ProducerRecord<String, SpecificRecord>(event.getTopic(), String.valueOf(pojo.tgChatId()), avro);
            record.headers().add("event-id", event.getEventId().toString().getBytes(StandardCharsets.UTF_8));

            return kafkaTemplate.send(record).thenApply(result -> event);
        } catch (Exception ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }
}

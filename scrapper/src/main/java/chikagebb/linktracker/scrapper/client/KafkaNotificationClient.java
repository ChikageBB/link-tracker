package chikagebb.linktracker.scrapper.client;

import chikagebb.linktracker.scrapper.properties.KafkaTopicProperties;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaNotificationClient implements NotificationClient {

    private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;
    private final KafkaTopicProperties props;

    @Override
    public void sendUpdate(Long id, String url, String description, List<Long> chatIds) {
        for (Long chatId : chatIds) {
            var avro = chikagebb.linktracker.avro.LinkUpdateEvent.newBuilder()
                    .setId(id)
                    .setUrl(url)
                    .setDescription(description)
                    .setTgChatId(chatId)
                    .build();

            var key = String.valueOf(chatId);
            var eventId = UUID.randomUUID();

            var rec =
                    new ProducerRecord<String, SpecificRecord>(props.getTopic().getName(), key, avro);
            rec.headers().add("event-id", eventId.toString().getBytes(StandardCharsets.UTF_8));

            kafkaTemplate.send(rec).whenComplete((res, ex) -> {
                if (ex != null) {
                    log.atError()
                            .setMessage("Failed to send link update to Kafka")
                            .addKeyValue("key", key)
                            .addKeyValue("eventId", eventId)
                            .setCause(ex)
                            .log();
                } else {
                    log.atDebug()
                            .setMessage("Sent message")
                            .addKeyValue("topic", res.getRecordMetadata().topic())
                            .addKeyValue("partition", res.getRecordMetadata().partition())
                            .addKeyValue("offset", res.getRecordMetadata().offset())
                            .log();
                }
            });
        }
    }
}

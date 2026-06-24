package chikagebb.linktracker.bot.kafka;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import chikagebb.linktracker.avro.LinkUpdateEvent;
import chikagebb.linktracker.bot.IntegrationTest;
import chikagebb.linktracker.bot.repository.ProcessedEventRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
public class KafkaUpdateListenerTest extends IntegrationTest {

    @Value("${app.kafka.topic.name}")
    String topic;

    @MockitoBean
    TelegramBot telegramBot;

    @Autowired
    ProcessedEventRepository processedEventRepository;

    @Test
    void messageWithoutEventIdHeaderGoesToDlt() {
        when(telegramBot.execute(any(SendMessage.class))).thenReturn(null);

        var avro = LinkUpdateEvent.newBuilder()
                .setId(1L)
                .setUrl("https://x.com")
                .setDescription("desc")
                .setTgChatId(42L)
                .build();

        var rec = new ProducerRecord<String, SpecificRecord>(topic, "42", avro);
        var producer = avroProducer();
        producer.send(rec);

        await().pollDelay(3, TimeUnit.SECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(telegramBot, never()).execute(any(SendMessage.class)));
    }

    @Test
    void consumerProcessesValidEvent() {
        when(telegramBot.execute(any(SendMessage.class))).thenReturn(null);

        var producer = avroProducer();

        var avro = LinkUpdateEvent.newBuilder()
                .setId(1L)
                .setUrl("https://github.com/test/repo")
                .setDescription("new commit")
                .setTgChatId(42L)
                .build();

        var rec = new ProducerRecord<String, SpecificRecord>(topic, "42", avro);
        rec.headers().add("event-id", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        producer.send(rec);

        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(telegramBot, times(1)).execute(any(SendMessage.class)));
    }

    private KafkaTemplate<String, SpecificRecord> avroProducer() {
        Map<String, Object> p = new HashMap<>();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, IntegrationTest.kafka.getBootstrapServers());
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        p.put("schema.registry.url", "mock://test-scope");
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(p));
    }
}

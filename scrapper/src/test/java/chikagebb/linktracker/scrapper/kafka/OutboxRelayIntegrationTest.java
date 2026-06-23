package chikagebb.linktracker.scrapper.kafka;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import chikagebb.linktracker.avro.LinkUpdateEvent;
import chikagebb.linktracker.scrapper.client.OutboxNotificationClient;
import chikagebb.linktracker.scrapper.relay.OutboxRelay;
import chikagebb.linktracker.scrapper.repository.orm.OutboxEventRepository;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public class OutboxRelayIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka-native:4.1.1"));

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    private static final String SCHEMA_REGISTRY_URL = "mock://outbox-test-" + UUID.randomUUID();
    private static final String TOPIC_NAME = "link-updates-" + UUID.randomUUID();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("app.kafka.schema-registry-url", () -> SCHEMA_REGISTRY_URL);
        registry.add("app.kafka.topic.name", () -> TOPIC_NAME);
        registry.add("app.communication.type", () -> "outbox");
        registry.add("app.scheduler.interval", () -> "1000");
    }

    @Autowired
    OutboxNotificationClient outboxClient;

    @Autowired
    OutboxRelay outboxRelay;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @Test
    @DisplayName("Событие сохраняется в outbox и публикуется в Kafka")
    void givenNewLinkUpdate_whenProcessed_thenPublishedToKafka() {
        outboxClient.sendUpdate(7L, "https://github.com/test/relay", "Outbox test", List.of(555L));

        assertThat(outboxEventRepository.count()).isEqualTo(1);
        var dbEvent = outboxEventRepository.findAll().get(0);
        assertThat(dbEvent.getPublishedAt()).isNull();
        UUID dbEventId = dbEvent.getEventId();

        outboxRelay.publish();

        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(
                        outboxEventRepository.findAll().get(0).getPublishedAt())
                .isNotNull());

        try (KafkaConsumer<String, LinkUpdateEvent> consumer = createTestConsumer()) {
            consumer.subscribe(List.of(TOPIC_NAME));
            ConsumerRecords<String, LinkUpdateEvent> records = consumer.poll(Duration.ofSeconds(10));

            assertThat(records).hasSize(1);
            ConsumerRecord<String, LinkUpdateEvent> record = records.iterator().next();

            LinkUpdateEvent event = record.value();
            assertThat(event.getId()).isEqualTo(7L);
            assertThat(event.getUrl()).isEqualTo("https://github.com/test/relay");
            assertThat(event.getDescription()).isEqualTo("Outbox test");
            assertThat(event.getTgChatId()).isEqualTo(555L);
            assertThat(record.key()).isEqualTo("555");

            var header = record.headers().lastHeader("event-id");
            assertThat(header).isNotNull();
            assertThat(new String(header.value(), UTF_8)).isEqualTo(dbEventId.toString());
        }
    }

    private KafkaConsumer<String, LinkUpdateEvent> createTestConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "outbox-test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put("schema.registry.url", SCHEMA_REGISTRY_URL);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        return new KafkaConsumer<>(props);
    }
}

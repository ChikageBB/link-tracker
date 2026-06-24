package chikagebb.linktracker.scrapper.kafka;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import chikagebb.linktracker.avro.LinkUpdateEvent;
import chikagebb.linktracker.scrapper.client.KafkaNotificationClient;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
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
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public class KafkaNotificationClientIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka-native:4.1.1"));

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    private static final String SCHEMA_REGISTRY_URL = "mock://test-registry-" + UUID.randomUUID();
    private static final String TOPIC_NAME = "link-updates-" + UUID.randomUUID();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("app.kafka.schema-registry-url", () -> SCHEMA_REGISTRY_URL);
        registry.add("app.kafka.topic.name", () -> TOPIC_NAME);
        registry.add("app.communication.type", () -> "kafka");
    }

    @Autowired
    KafkaNotificationClient notificationClient;

    @BeforeEach
    void setupSchemaRegistry() throws RestClientException, IOException {
        String scope = SCHEMA_REGISTRY_URL.replace("mock://", "");
        MockSchemaRegistry.dropScope(scope);
        SchemaRegistryClient client = MockSchemaRegistry.getClientForScope(scope);
        client.register(TOPIC_NAME + "-value", new AvroSchema(LinkUpdateEvent.getClassSchema()));
    }

    @Test
    void shouldProduceOneAvroMessagePerChat() {
        notificationClient.sendUpdate(42L, "https://github.com/test/repo", "Test push event", List.of(100L, 200L));

        try (KafkaConsumer<String, LinkUpdateEvent> consumer = createTestConsumer()) {
            consumer.subscribe(List.of(TOPIC_NAME));
            ConsumerRecords<String, LinkUpdateEvent> records = consumer.poll(Duration.ofSeconds(10));

            assertThat(records).hasSize(2);
            Set<String> keys = new HashSet<>();
            Set<Long> chatIds = new HashSet<>();
            Set<String> eventIds = new HashSet<>();

            for (ConsumerRecord<String, LinkUpdateEvent> record : records) {
                keys.add(record.key());

                LinkUpdateEvent event = record.value();
                assertThat(event.getId()).isEqualTo(42L);
                assertThat(event.getUrl()).isEqualTo("https://github.com/test/repo");
                assertThat(event.getDescription()).isEqualTo("Test push event");
                chatIds.add(event.getTgChatId());

                var header = record.headers().lastHeader("event-id");
                assertThat(header).isNotNull();
                eventIds.add(new String(header.value(), UTF_8));
            }

            assertThat(keys).containsExactlyInAnyOrder("100", "200");
            assertThat(chatIds).containsExactlyInAnyOrder(100L, 200L);
            assertThat(eventIds).hasSize(2);
        }
    }

    private KafkaConsumer<String, LinkUpdateEvent> createTestConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put("schema.registry.url", SCHEMA_REGISTRY_URL);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        return new KafkaConsumer<>(props);
    }
}

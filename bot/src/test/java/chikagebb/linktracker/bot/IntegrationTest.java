package chikagebb.linktracker.bot;

import java.util.UUID;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class IntegrationTest {

    public static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    public static final ConfluentKafkaContainer kafka =
            new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    static {
        postgres.start();
        kafka.start();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("app.kafka.schema-registry-url", () -> "mock://test-scope");
        registry.add("app.kafka.topic.group-id", () -> "test-" + UUID.randomUUID());
        registry.add("app.kafka.topic.name", () -> "link-updates-" + UUID.randomUUID());
    }
}

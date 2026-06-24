package chikagebb.linktracker.scrapper.config;

import chikagebb.linktracker.scrapper.client.BotGrpcClient;
import chikagebb.linktracker.scrapper.client.BotHttpClient;
import chikagebb.linktracker.scrapper.client.KafkaNotificationClient;
import chikagebb.linktracker.scrapper.client.OutboxNotificationClient;
import chikagebb.linktracker.scrapper.client.bot.api.UpdatesApi;
import chikagebb.linktracker.scrapper.properties.KafkaTopicProperties;
import chikagebb.linktracker.scrapper.repository.orm.OutboxEventRepository;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class NotificationClientConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.communication", name = "type", havingValue = "http", matchIfMissing = false)
    public BotHttpClient httpNotificationClient(UpdatesApi api) {
        return new BotHttpClient(api);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.communication", name = "type", havingValue = "grpc", matchIfMissing = true)
    public BotGrpcClient grpcNotificationClient(GrpcChannelFactory factory) {
        return new BotGrpcClient(factory);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.communication", name = "type", havingValue = "kafka", matchIfMissing = false)
    public KafkaNotificationClient kafkaNotificationClient(
            KafkaTemplate<String, SpecificRecord> kafkaTemplate, KafkaTopicProperties props) {
        return new KafkaNotificationClient(kafkaTemplate, props);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.communication", name = "type", havingValue = "outbox", matchIfMissing = false)
    public OutboxNotificationClient outboxNotificationClient(
            OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper, KafkaTopicProperties props) {
        return new OutboxNotificationClient(outboxEventRepository, objectMapper, props);
    }
}

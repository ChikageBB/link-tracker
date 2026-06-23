package chikagebb.linktracker.scrapper.config;

import chikagebb.linktracker.scrapper.properties.KafkaTopicProperties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(KafkaTopicProperties.class)
@ConditionalOnExpression("'${app.communication.type}'.equals('kafka') || '${app.communication.type}'.equals('outbox')")
public class KafkaConfig {

    private final KafkaTopicProperties props;

    @Bean
    public NewTopic linkUpdatesTopic() {
        return TopicBuilder.name(props.getTopic().getName())
                .partitions(props.getTopic().getPartitions())
                .replicas(props.getTopic().getReplicas())
                .config(
                        TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG,
                        String.valueOf(props.getTopic().getMinInsyncReplicas()))
                .config(
                        TopicConfig.RETENTION_MS_CONFIG,
                        String.valueOf(props.getTopic().getRetentionMs()))
                .build();
    }

    @Bean
    public NewTopic linkUpdatesDlq() {
        return TopicBuilder.name(props.getTopic().getName() + ".dlq")
                .partitions(1)
                .replicas(props.getTopic().getReplicas())
                .build();
    }
}

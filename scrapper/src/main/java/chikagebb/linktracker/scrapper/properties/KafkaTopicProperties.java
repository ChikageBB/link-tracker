package chikagebb.linktracker.scrapper.properties;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@NoArgsConstructor
@ConfigurationProperties(prefix = "app.kafka.topic")
public class KafkaTopicProperties {

    @NotEmpty(message = "Имя топика не должно быть пустым")
    private String name;

    @Positive(message = "Количество партиций должно быть больше 0")
    private int partitions;

    @Positive(message = "Количество реплик должно быть больше 0")
    private int replicas;

    @Positive(message = "Минимальное количество синхронных реплик должно быть больше 0")
    private int minInsyncReplicas;
}

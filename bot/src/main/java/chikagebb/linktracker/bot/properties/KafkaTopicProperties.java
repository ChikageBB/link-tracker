package chikagebb.linktracker.bot.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@NoArgsConstructor
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaTopicProperties {

    @Valid
    private Topic topic;

    @Valid
    private Retry retry;

    @NotBlank(message = "URL Schema Registry не должен быть пустым")
    private String schemaRegistryUrl;

    @Getter
    @Setter
    public static class Topic {

        @NotEmpty(message = "Имя топика не должно быть пустым")
        private String name;

        @Positive(message = "Количество партиций должно быть больше 0")
        private int partitions;

        @NotEmpty(message = "id группы должно быть больше 0")
        private String groupId;
    }

    @Getter
    @Setter
    public static class Retry {

        @Positive(message = "Максимальное число попыток должно быть больше 0")
        private int maxAttempts;
    }
}

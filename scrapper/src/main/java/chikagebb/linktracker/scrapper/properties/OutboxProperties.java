package chikagebb.linktracker.scrapper.properties;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@NoArgsConstructor
@ConfigurationProperties(prefix = "app.outbox")
public class OutboxProperties {

    @NotNull(message = "Интервал не должен быть пустым")
    private String interval;

    @Positive(message = "Количество попыток должно быть больше 0")
    private int maxAttempts;
}

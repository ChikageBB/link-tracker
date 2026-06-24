package chikagebb.linktracker.scrapper.properties;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Setter
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@Validated
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {

    @NotNull(message = "Время жизни кэша должно быть указано")
    private Duration linksTtl;

    @NotNull(message = "Флаг включения кэша должен быть указан")
    protected Boolean enabled;
}

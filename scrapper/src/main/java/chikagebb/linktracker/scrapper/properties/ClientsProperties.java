package chikagebb.linktracker.scrapper.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "clients")
public record ClientsProperties(
        @Valid @NotNull(message = "Настройки бота не должны быть пустыми")
        ClientProperties bot,

        @Valid @NotNull(message = "Настройки скраппера не должны быть пустыми")
        ClientProperties scrapper) {

    public record ClientProperties(
            @NotBlank(message = "Базовый URL (baseUrl) не должен быть пустым")
            @URL(message = "Некорректный формат URL-адреса")
            String baseUrl,

            @NotNull(message = "connectTimeout должен быть указан")
            Duration connectTimeout,

            @NotNull(message = "readTimeout должен быть указан")
            Duration readTimeout) {}
}

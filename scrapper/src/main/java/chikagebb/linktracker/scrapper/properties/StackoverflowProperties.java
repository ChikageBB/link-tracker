package chikagebb.linktracker.scrapper.properties;

import jakarta.validation.constraints.NotEmpty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@Getter
@Setter
@EqualsAndHashCode
@ConfigurationProperties(prefix = "app.stackoverflow")
public class StackoverflowProperties {

    @NotEmpty(message = "Ключ не должен быть пустым")
    private String key;

    @NotEmpty(message = "Токен доступа (accessToken) не должен быть пустым")
    private String accessToken;

    @NotEmpty(message = "Базовый URL (baseUrl) не должен быть пустым")
    private String baseUr;
}

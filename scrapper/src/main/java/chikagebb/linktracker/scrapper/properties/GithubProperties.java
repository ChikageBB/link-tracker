package chikagebb.linktracker.scrapper.properties;

import jakarta.validation.constraints.NotEmpty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@NoArgsConstructor
@EqualsAndHashCode
@ConfigurationProperties(prefix = "app.github")
public class GithubProperties {

    @NotEmpty(message = "Токен (token) не должен быть пустым")
    private String token;

    @NotEmpty(message = "Базовый URL (baseUrl) не должен быть пустым")
    private String baseUr;
}

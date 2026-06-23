package chikagebb.linktracker.bot.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clients")
public record ClientsProperties(ClientProperties bot, ClientProperties scrapper) {

    public record ClientProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {}
}

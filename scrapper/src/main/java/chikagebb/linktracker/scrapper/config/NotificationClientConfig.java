package chikagebb.linktracker.scrapper.config;

import chikagebb.linktracker.scrapper.client.BotGrpcClient;
import chikagebb.linktracker.scrapper.client.BotHttpClient;
import chikagebb.linktracker.scrapper.client.bot.api.UpdatesApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class NotificationClientConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.communication", name = "type", havingValue = "http", matchIfMissing = true)
    public BotHttpClient httpNotificationClient(UpdatesApi api) {
        return new BotHttpClient(api);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "app.communication", name = "type", havingValue = "grpc", matchIfMissing = true)
    public BotGrpcClient grpcNotificationClient(GrpcChannelFactory factory) {
        return new BotGrpcClient(factory);
    }
}

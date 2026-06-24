package chikagebb.linktracker.scrapper.config;

import chikagebb.linktracker.scrapper.client.bot.api.UpdatesApi;
import chikagebb.linktracker.scrapper.client.bot.invoker.ApiClient;
import chikagebb.linktracker.scrapper.properties.ClientsProperties;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BotClientConfig {

    @Bean
    public ApiClient botApiClient(RestTemplateBuilder builder, ClientsProperties props) {
        var restTemplate = builder.connectTimeout(props.bot().connectTimeout())
                .readTimeout(props.bot().readTimeout())
                .build();

        var client = new ApiClient(restTemplate);
        client.setBasePath(props.bot().baseUrl());
        return client;
    }

    @Bean
    public UpdatesApi updatesApi(ApiClient botApiClient) {
        return new UpdatesApi(botApiClient);
    }
}

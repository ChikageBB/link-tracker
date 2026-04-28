package chikagebb.linktracker.bot.config;

import chikagebb.linktracker.bot.client.scrapper.api.LinksApi;
import chikagebb.linktracker.bot.client.scrapper.api.TgChatApi;
import chikagebb.linktracker.bot.client.scrapper.invoker.ApiClient;
import chikagebb.linktracker.bot.properties.ClientsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ClientsProperties.class)
public class ScrapperClientConfig {

    @Bean
    public ApiClient botApiClient(RestTemplateBuilder builder, ClientsProperties properties) {
        var restTemplate = builder.connectTimeout(properties.scrapper().connectTimeout())
                .readTimeout(properties.scrapper().readTimeout())
                .build();

        var client = new ApiClient(restTemplate);
        client.setBasePath(properties.scrapper().baseUrl());
        return client;
    }

    @Bean
    public TgChatApi tgChatApi(ApiClient scrapperApiClient) {
        return new TgChatApi(scrapperApiClient);
    }

    @Bean
    public LinksApi linksApi(ApiClient scrapperApiClient) {
        return new LinksApi(scrapperApiClient);
    }
}

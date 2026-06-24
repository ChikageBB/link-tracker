package chikagebb.linktracker.scrapper.config;

import chikagebb.linktracker.scrapper.properties.GithubProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GithubConfig {

    @Bean
    public RestClient githubRestClient(GithubProperties props) {
        return RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getToken())
                .defaultHeader("Accept", "application/json")
                .build();
    }
}

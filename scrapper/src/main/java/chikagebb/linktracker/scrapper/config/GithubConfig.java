package chikagebb.linktracker.scrapper.config;

import chikagebb.linktracker.scrapper.properties.GithubProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GithubProperties.class)
public class GithubConfig {

    @Bean
    public RestClient githubRestClient(GithubProperties props) {
        return RestClient.builder()
                .baseUrl(props.getBaseUr())
                .defaultHeader("Authorization", "Bearer " + props.getToken())
                .defaultHeader("Accept", "application/json")
                .build();
    }
}

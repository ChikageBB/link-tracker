package chikagebb.linktracker.scrapper.linkchecker;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import chikagebb.linktracker.scrapper.checker.StackOverflowLinkChecker;
import chikagebb.linktracker.scrapper.client.StackOverflowClient;
import chikagebb.linktracker.scrapper.dto.LinkDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@EnableWireMock(@ConfigureWireMock(name = "stackoverflow", baseUrlProperties = "app.stackoverflow.base-url"))
public class StackOverflowLinkCheckerTest {

    @InjectWireMock("stackoverflow")
    private WireMockServer wireMock;

    @Autowired
    private StackOverflowLinkChecker checker;

    @Autowired
    private ObjectMapper objectMapper;

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.username", postgres::getUsername);
    }

    @BeforeEach
    void setUp() {
        wireMock.stubFor(get(anyUrl()).willReturn(okJson("{\"items\": []}")));
    }

    @Test
    void check_whenNewAnswer_returnsFormattedMessage() throws JsonProcessingException {
        OffsetDateTime currentTime = OffsetDateTime.now();
        long nowEpoch = currentTime.toEpochSecond();

        var question = new StackOverflowClient.StackOverflowResponse(
                List.of(new StackOverflowClient.StackOverflowItem("How to use Spring?", null, null, null)));

        var answers = new StackOverflowClient.StackOverflowResponse(List.of(new StackOverflowClient.StackOverflowItem(
                null, nowEpoch, new StackOverflowClient.StackOverflowOwner("expert_user"), "<p>Use @Autowired</p>")));

        wireMock.stubFor(get(urlPathEqualTo("/questions/12345"))
                .atPriority(1)
                .willReturn(okJson(objectMapper.writeValueAsString(question))));

        wireMock.stubFor(get(urlPathEqualTo("/questions/12345/answers"))
                .atPriority(1)
                .willReturn(okJson(objectMapper.writeValueAsString(answers))));

        LinkDto link =
                new LinkDto(1L, URI.create("https://stackoverflow.com/questions/12345/how-to-use-spring"), List.of());

        link.setLastCheckAt(currentTime.minusHours(1));

        Optional<String> result = checker.check(link);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow(
                        () -> new AssertionError("Checker вернул пустой Optional - обновление не было найдено")))
                .contains("How to use Spring?")
                .contains("expert_user")
                .contains("Use @Autowired");

        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/questions/12345")));
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/questions/12345/answers")));
    }

    @Test
    void check_whenApiUnavailable_returnsEmpty() {
        wireMock.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(503)));

        LinkDto link = new LinkDto(1L, URI.create("https://stackoverflow.com/questions/12345/test"), List.of());

        link.setLastCheckAt(OffsetDateTime.now().minusHours(1));

        Optional<String> result = checker.check(link);

        assertThat(result).isEmpty();
    }
}

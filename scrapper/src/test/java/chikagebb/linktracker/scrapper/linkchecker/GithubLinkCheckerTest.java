package chikagebb.linktracker.scrapper.linkchecker;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

import chikagebb.linktracker.scrapper.checker.GithubLinkChecker;
import chikagebb.linktracker.scrapper.client.GithubClient;
import chikagebb.linktracker.scrapper.dto.LinkDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.AssertionsForClassTypes;
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
@EnableWireMock({@ConfigureWireMock(name = "github", baseUrlProperties = "app.github.base-url")})
public class GithubLinkCheckerTest {

    @InjectWireMock("github")
    private WireMockServer wireMock;

    @Autowired
    private GithubLinkChecker checker;

    @Autowired
    private ObjectMapper objectMapper;

    private LinkDto link;

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
        link = new LinkDto(1L, URI.create("https://github.com/owner/repo"), List.of());
        link.setLastCheckAt(OffsetDateTime.now().minusHours(1));

        wireMock.stubFor(get(anyUrl()).willReturn(okJson("[]")));
    }

    @Test
    void check_whenNewIssue_returnFormattedMessage() throws JsonProcessingException {

        var issue = new GithubClient.GithubItem(
                "Fix bug",
                OffsetDateTime.parse("2026-04-04T20:00:00Z"),
                new GithubClient.GithubUser("testuser"),
                "Some issue body");

        wireMock.stubFor(get(urlPathMatching("/repos/owner/repo/issues"))
                .willReturn(okJson(objectMapper.writeValueAsString(List.of(issue)))));

        Optional<String> result = checker.check(link);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow(
                        () -> new AssertionError("Checker вернул пустой Optional - обновление не было найдено")))
                .contains("Issue")
                .contains("testuser")
                .contains(OffsetDateTime.parse("2026-04-04T20:00:00Z")
                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                        .toString())
                .contains("Some issue body");
    }

    @Test
    void check_whenNewPush_returnFormattedMessage() throws JsonProcessingException {

        var push = new GithubClient.GithubCommit(
                new GithubClient.GithubCommitData(
                        "feat: add feature",
                        new GithubClient.GithubCommitAuthor("Roman", OffsetDateTime.parse("2026-04-04T20:00:00Z"))),
                new GithubClient.GithubUser("roman"));

        wireMock.stubFor(get(urlPathMatching("/repos/owner/repo/commits"))
                .willReturn(okJson(objectMapper.writeValueAsString(List.of(push)))));

        Optional<String> result = checker.check(link);

        AssertionsForClassTypes.assertThat(result).isPresent();
        AssertionsForClassTypes.assertThat(result.orElseThrow(
                        () -> new AssertionError("Checker вернул пустой Optional - обновление не было найдено")))
                .contains("Push")
                .contains("feat: add feature")
                .contains("Roman");
    }

    @Test
    void check_whenPreviewLongerThan200_truncatesTo200() throws JsonProcessingException {
        String longBody = "a".repeat(300);

        var issue = new GithubClient.GithubItem(
                "Long issue",
                OffsetDateTime.parse("2026-04-04T20:00:00Z"),
                new GithubClient.GithubUser("user"),
                longBody);

        wireMock.stubFor(get(urlPathMatching("/repos/owner/repo/issues"))
                .willReturn(okJson(objectMapper.writeValueAsString(List.of(issue)))));

        Optional<String> result = checker.check(link);

        AssertionsForClassTypes.assertThat(result).isPresent();
        AssertionsForClassTypes.assertThat(result.orElseThrow(
                        () -> new AssertionError("Checker вернул пустой Optional - обновление не было найдено")))
                .contains("a".repeat(200) + "...");
        AssertionsForClassTypes.assertThat(result.orElseThrow()).doesNotContain("a".repeat(300));
    }

    @Test
    void check_whenApiUnavailable_returnsEmpty() {
        wireMock.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(503)));

        Optional<String> result = checker.check(link);

        AssertionsForClassTypes.assertThat(result).isEmpty();
    }

    @Test
    void check_whenNoNewEvents_returnsEmpty() {
        Optional<String> result = checker.check(link);

        AssertionsForClassTypes.assertThat(result).isEmpty();
    }
}

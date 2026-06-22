package chikagebb.linktracker.scrapper.repository;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import chikagebb.linktracker.scrapper.client.NotificationClient;
import chikagebb.linktracker.scrapper.dto.LinkDto;
import chikagebb.linktracker.scrapper.scheduler.LinkUpdateScheduler;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@EnableWireMock(@ConfigureWireMock(name = "github", baseUrlProperties = "app.github.base-url"))
public class BatchProcessingTest {

    @InjectWireMock("github")
    private WireMockServer wireMock;

    @Autowired
    private LinkUpdateScheduler scheduler;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private ChatRepository chatRepository;

    @MockitoBean
    private NotificationClient notificationClient;

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void setUp() {
        chatRepository.save(1L);
        chatRepository.save(2L);
    }

    @Test
    void processBatch_whenPartialFailure_otherLinksStillProcessed() {
        wireMock.stubFor(
                get(urlPathMatching("/repos/owner1/.*")).willReturn(aResponse().withStatus(503)));

        wireMock.stubFor(
                get(urlPathMatching("/repos/owner2/repo/commits")).atPriority(1).willReturn(okJson("""
                        [{
                            "commit": {
                                "message": "fix: something",
                                "author": {"name": "user", "date": "2026-04-04T20:00:00Z"}
                            }
                        }]
                    """)));
        wireMock.stubFor(get(urlPathMatching("/repos/owner2/.*")).atPriority(2).willReturn(okJson("[]")));

        LinkDto link1 = linkRepository.save(1L, URI.create("https://github.com/owner1/repo"), List.of());
        linkRepository.updateLastCheckAt(link1.getId(), OffsetDateTime.now().minusHours(1));

        LinkDto link2 = linkRepository.save(2L, URI.create("https://github.com/owner2/repo"), List.of());
        linkRepository.updateLastCheckAt(link2.getId(), OffsetDateTime.now().minusHours(1));

        scheduler.checkUpdates();

        await().atMost(5, SECONDS)
                .untilAsserted(() -> verify(notificationClient, atLeastOnce()).sendUpdate(any(), any(), any(), any()));
    }
}

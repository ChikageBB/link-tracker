package chikagebb.linktracker.bot;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import chikagebb.linktracker.bot.initializer.TelegramCommandInitializer;
import chikagebb.linktracker.bot.listener.TelegramUpdateListener;
import chikagebb.linktracker.bot.properties.TelegramProperties;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.request.GetUpdates;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.EnableWireMock;

@SpringBootTest
@EnableWireMock
@ActiveProfiles("test")
public class TelegramBotIntegrationTest {

    @MockitoBean
    TelegramCommandInitializer commandInitializer;

    @MockitoBean
    TelegramUpdateListener telegramUpdateListener;

    @Autowired
    TelegramBot telegramBot;

    @Autowired
    TelegramProperties telegramProperties;

    @AfterEach
    void clearUpdatesListener() {
        telegramBot.removeGetUpdatesListener();
    }

    @Nested
    @DisplayName("Негативные сценарии")
    class NegativeScenarios {

        @Test
        void nonExistingTokenRequest() {
            stubFor(post(urlMatching("/bot[^/]+/getUpdates"))
                    .willReturn(aResponse()
                            .withStatus(404)
                            .withBody("{\"ok\":false,\"error_code\":404,\"description\":\"Not Found\"}")));

            var getUpdatesRequest = new GetUpdates();
            var getUpdatesResponse = telegramBot.execute(getUpdatesRequest);

            assertFalse(getUpdatesResponse.isOk());
            assertEquals(404, getUpdatesResponse.errorCode());

            verify(
                    1,
                    postRequestedFor(urlPathTemplate("/bot{token}/getUpdates"))
                            .withPathParam("token", equalTo(telegramProperties.getToken())));
        }

        @Test
        void updatesListenerReceivesUpdates() throws InterruptedException {
            stubFor(post(urlMatching("/bot[^/]+/getUpdates"))
                    .inScenario("Updates Listener")
                    .whenScenarioStateIs(STARTED)
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody("""
                        {
                          "ok": true,
                          "result": [
                            {
                              "update_id": 123456,
                              "message": {
                                "message_id": 1,
                                "from": {
                                  "id": 987654321,
                                  "is_bot": false,
                                  "first_name": "Test",
                                  "username": "testuser"
                                },
                                "chat": {
                                  "id": 987654321,
                                  "type": "private"
                                },
                                "date": 1234567890,
                                "text": "Hello Bot"
                              }
                            }
                          ]
                        }
                        """))
                    .willSetStateTo("Updates Received"));

            stubFor(post(urlMatching("/bot[^/]+/getUpdates"))
                    .inScenario("Update Listener")
                    .whenScenarioStateIs("Updates Received")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody("""
                        {
                          "ok": true,
                          "result": []
                        }
                        """)));

            List<Update> receivedUpdates = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);

            telegramBot.setUpdatesListener(updates -> {
                receivedUpdates.addAll(updates);
                latch.countDown();
                return UpdatesListener.CONFIRMED_UPDATES_ALL;
            });

            boolean received = latch.await(10, TimeUnit.SECONDS);

            assertTrue(received);
            assertThat(receivedUpdates)
                    .hasSize(1)
                    .first()
                    .returns(123456, Update::updateId)
                    .extracting(Update::message)
                    .returns("Hello Bot", Message::text)
                    .extracting(Message::from)
                    .returns("testuser", User::username);

            verify(postRequestedFor(urlPathTemplate("/bot{token}/getUpdates"))
                    .withPathParam("token", equalTo(telegramProperties.getToken())));
        }
    }
}

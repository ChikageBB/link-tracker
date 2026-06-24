package chikagebb.linktracker.scrapper.client;

import chikagebb.linktracker.scrapper.properties.StackoverflowProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class StackOverflowClient {

    private final StackoverflowProperties props;

    public Optional<StackOverflowEvent> getLastAnswer(Long questionId, OffsetDateTime since) {
        String questionTitle = getQuestionTitle(questionId);

        if (questionTitle == null) return Optional.empty();

        long fromDate = since.toEpochSecond();

        try {
            var response = RestClient.create()
                    .get()
                    .uri(
                            props.getBaseUrl()
                                    + "/questions/{id}/answers?site=stackoverflow&sort=creation&order=desc&pagesize=1&fromdate={from}&key={key}",
                            questionId,
                            fromDate,
                            props.getKey())
                    .retrieve()
                    .body(StackOverflowResponse.class);

            if (response == null || response.items == null || response.items.isEmpty()) {
                return Optional.empty();
            }

            var answer = response.items.getFirst();
            return Optional.of(new StackOverflowEvent(
                    questionTitle,
                    answer.owner.displayName,
                    Instant.ofEpochSecond(answer.creatingDate).atOffset(ZoneOffset.UTC),
                    preview(answer.body)));
        } catch (Exception e) {
            log.error("Ошибка получения ответов SO {}: {}", questionId, e.getMessage());
            return Optional.empty();
        }
    }

    public String getQuestionTitle(Long questiongId) {
        try {
            var response = RestClient.create()
                    .get()
                    .uri(
                            props.getBaseUrl() + "/questions/{id}?site=stackoverflow&key={key}",
                            questiongId,
                            props.getKey())
                    .retrieve()
                    .body(StackOverflowResponse.class);

            if (response == null || response.items == null || response.items.isEmpty()) {
                return null;
            }

            return response.items.getFirst().title;
        } catch (Exception e) {
            log.error("Ошибка получения вопроса SO: {}: {}", questiongId, e.getMessage());
            return null;
        }
    }

    private String preview(String body) {
        if (body == null) return "";
        String plain = body.replaceAll("<[^>]+>", "");
        return plain.length() > 200 ? plain.substring(0, 200) + "..." : plain;
    }

    public record StackOverflowEvent(
            String questionTitle, String author, OffsetDateTime createdAt, String bodyPreview) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StackOverflowResponse(List<StackOverflowItem> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StackOverflowItem(
            String title,
            @JsonProperty("creation_date") Long creatingDate,
            @JsonProperty("owner") StackOverflowOwner owner,
            String body) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StackOverflowOwner(
            @JsonProperty("display_name") String displayName) {}
}

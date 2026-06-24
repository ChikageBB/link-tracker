package chikagebb.linktracker.scrapper.client;

import chikagebb.linktracker.scrapper.properties.GithubProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class GithubClient {

    private final GithubProperties githubProperties;
    private final RestClient restClient;

    public Optional<GithubEvent> getLatestEvent(String owner, String repo, OffsetDateTime since) {
        Optional<GithubEvent> latestIssue = getLastIssue(owner, repo, since);
        Optional<GithubEvent> latestPR = getLastPr(owner, repo, since);
        Optional<GithubEvent> latestPush = getLastPush(owner, repo, since);

        return Stream.of(latestIssue, latestPR, latestPush)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Comparator.comparing(GithubEvent::createdAt));
    }

    private Optional<GithubEvent> getLastIssue(String owner, String repo, OffsetDateTime since) {
        try {
            var items = get(
                    "/repos/{owner}/{repo}/issues?state=all&per_page=1&sort=created&direction=desc&since={since}",
                    GithubItem[].class,
                    owner,
                    repo,
                    since.toString());

            if (items == null || items.length == 0) return Optional.empty();

            var item = items[0];

            return Optional.of(
                    new GithubEvent("Issue", item.title, item.user.login, item.createdAt, preview(item.body)));
        } catch (Exception e) {
            log.error("Ошибка получения Issue {}/{} : {}", owner, repo, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<GithubEvent> getLastPr(String owner, String repo, OffsetDateTime since) {
        try {
            var items = get(
                    "/repos/{owner}/{repo}/pulls?state=all&per_page=1&sort=created&direction=desc",
                    GithubItem[].class,
                    owner,
                    repo);

            if (items == null || items.length == 0) return Optional.empty();

            var item = items[0];

            if (!item.createdAt.isAfter(since)) return Optional.empty();

            return Optional.of(new GithubEvent("PR", item.title, item.user.login, item.createdAt, preview(item.body)));
        } catch (Exception e) {
            log.error("Ошибка получения PR {}/{} : {}", owner, repo, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<GithubEvent> getLastPush(String owner, String repo, OffsetDateTime since) {
        try {
            var items = get(
                    "/repos/{owner}/{repo}/commits?per_page=1&since={since}",
                    GithubCommit[].class,
                    owner,
                    repo,
                    since.toString());

            if (items == null || items.length == 0) return Optional.empty();

            var item = items[0];
            return Optional.of(new GithubEvent(
                    "Push",
                    item.commit.message,
                    item.commit.author.name,
                    item.commit.author.date,
                    preview(item.commit.message)));
        } catch (Exception e) {
            log.error("Ошибка получения Push {}/{} : {}", owner, repo, e.getMessage());
            return Optional.empty();
        }
    }

    private <T> T get(String uri, Class<T> responseType, Object... uriArgs) {
        return restClient.get().uri(uri, uriArgs).retrieve().body(responseType);
    }

    private String preview(String body) {
        if (body == null) return "";
        return body.length() > 200 ? body.substring(0, 200) + "..." : body;
    }

    public record GithubEvent(String type, String title, String username, OffsetDateTime createdAt, String preview) {}

    public record GithubItem(
            String title, @JsonProperty("created_at") OffsetDateTime createdAt, GithubUser user, String body) {}

    public record GithubCommit(GithubCommitData commit, GithubUser user) {}

    public record GithubCommitData(String message, GithubCommitAuthor author) {}

    public record GithubCommitAuthor(
            String name, @JsonProperty("date") OffsetDateTime date) {}

    public record GithubUser(String login) {}
}

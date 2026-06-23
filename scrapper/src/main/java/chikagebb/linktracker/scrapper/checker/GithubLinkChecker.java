package chikagebb.linktracker.scrapper.checker;

import chikagebb.linktracker.scrapper.client.GithubClient;
import chikagebb.linktracker.scrapper.dto.LinkDto;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GithubLinkChecker implements LinkChecker {

    private static final Pattern GITHUB_PATTERN = Pattern.compile("github\\.com/([^/]+)/([^/]+)");
    private final GithubClient githubClient;

    @Override
    public boolean support(URI url) {
        return GITHUB_PATTERN.matcher(url.toString()).find();
    }

    @Override
    public Optional<String> check(LinkDto linkDto) {
        Matcher matcher = GITHUB_PATTERN.matcher(linkDto.getUrl().toString());

        if (!matcher.find()) return Optional.empty();

        String owner = matcher.group(1);
        String repo = matcher.group(2);

        return githubClient
                .getLatestEvent(owner, repo, linkDto.getLastCheckAt())
                .map(event -> formatMessage(linkDto.getUrl().toString(), event));
    }

    private String formatMessage(String url, GithubClient.GithubEvent event) {
        return """
            🚀 Новый %s в %s%n\
            %n\
            👤 %s%n\
            🕐 %s%n\
            📝 %s%n\
            """.formatted(
                        event.type(),
                        url,
                        event.username(),
                        event.createdAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                        event.preview());
    }
}

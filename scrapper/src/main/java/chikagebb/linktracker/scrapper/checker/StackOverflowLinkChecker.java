package chikagebb.linktracker.scrapper.checker;

import chikagebb.linktracker.scrapper.client.StackOverflowClient;
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
public class StackOverflowLinkChecker implements LinkChecker {

    private static final Pattern STACKOVERFLOW_PATTERN = Pattern.compile("stackoverflow\\.com/question/(\\d+)");
    private final StackOverflowClient stackOverflowClient;

    @Override
    public boolean support(URI url) {
        return STACKOVERFLOW_PATTERN.matcher(url.toString()).find();
    }

    @Override
    public Optional<String> check(LinkDto linkDto) {
        Matcher matcher = STACKOVERFLOW_PATTERN.matcher(linkDto.getUrl().toString());

        if (!matcher.find()) return Optional.empty();

        Long questionId = Long.parseLong(matcher.group(1));

        return stackOverflowClient
                .getLastAnswer(questionId, linkDto.getLastCheckAt())
                .map(event -> formatMessage(linkDto.getUrl().toString(), event));
    }

    private String formatMessage(String url, StackOverflowClient.StackOverflowEvent event) {
        return """
             💡 Новый ответ на StackOverflow%n\
             %n\
             ❓ %s%n\
             %n\
             👤 Автор: %s%n\
             🕐 %s%n\
             %n\
             📝 %s%n\
             %n\
             🔗 %s%n\
            """.formatted(
                        event.questionTitle(),
                        event.author(),
                        event.createdAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                        event.bodyPreview(),
                        url);
    }
}

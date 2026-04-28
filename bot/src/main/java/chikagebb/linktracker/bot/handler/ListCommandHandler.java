package chikagebb.linktracker.bot.handler;

import chikagebb.linktracker.bot.client.ScrapperClient;
import chikagebb.linktracker.bot.command.TelegramCommand;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ListCommandHandler implements UpdateHandler {

    private final ScrapperClient scrapperClient;

    private static final String NO_TRACKING_LINKS_MESSAGE = """
        📭 У тебя пока нет отслеживаемых ссылок

        Добавь первую:
        /track <ссылка>
        """;

    private static final String TRACKING_LINK_MESSAGE = "📌 Твои отслеживаемые ссылки:%n%n%s";

    @Override
    public boolean isSupport(Update update) {
        if (update.message() == null || update.message().text() == null) {
            return false;
        }

        String text = update.message().text();
        String commandValue = TelegramCommand.LIST.getValue();

        return TelegramCommand.LIST.isEnabled() && (commandValue.equals(text) || text.startsWith(commandValue + " "));
    }

    @Override
    public void handle(Update update, TelegramBot bot) {
        Long chatId = update.message().chat().id();
        String text = update.message().text();

        String tag = text.contains(" ") ? text.substring(text.indexOf(" ") + 1).trim() : null;

        var response = scrapperClient.getLinks(chatId);
        var links = response.getLinks();

        if (links == null || links.isEmpty()) {
            bot.execute(new SendMessage(chatId, NO_TRACKING_LINKS_MESSAGE));
            return;
        }

        var filtered = tag == null
                ? links
                : links.stream()
                        .filter(l -> l.getTags() != null && l.getTags().contains(tag))
                        .toList();

        String message = filtered.stream()
                .map(l -> "• " + l.getUrl() + " "
                        + (l.getTags() == null ? "" : "[" + String.join(", ", l.getTags()) + "]"))
                .collect(Collectors.joining("\n"));

        log.atInfo()
                .setMessage("User requested list")
                .addKeyValue("chat_id", chatId)
                .addKeyValue("tag", tag)
                .log();

        bot.execute(new SendMessage(chatId, TRACKING_LINK_MESSAGE.formatted(message)));
    }
}

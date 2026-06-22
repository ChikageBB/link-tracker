package chikagebb.linktracker.scrapper.client;

import chikagebb.linktracker.scrapper.client.bot.api.UpdatesApi;
import chikagebb.linktracker.scrapper.client.bot.model.LinkUpdate;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class BotClient implements NotificationClient {

    private final UpdatesApi updatesApi;

    @Override
    public void sendUpdate(Long id, String url, String description, List<Long> chatIds) {

        var update = new LinkUpdate();
        update.setId(id);
        update.setUrl(URI.create(url));
        update.setDescription(description);
        update.setTgChatIds(chatIds);

        try {
            updatesApi.sendUpdate(update);
        } catch (Exception e) {
            log.error("Ошибка отправки обновления боту: {}", url, e);
        }
    }
}

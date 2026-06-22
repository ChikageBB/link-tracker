package chikagebb.linktracker.bot.client;

import chikagebb.linktracker.bot.client.scrapper.api.LinksApi;
import chikagebb.linktracker.bot.client.scrapper.api.TgChatApi;
import chikagebb.linktracker.bot.client.scrapper.model.AddLinkRequest;
import chikagebb.linktracker.bot.client.scrapper.model.LinkResponse;
import chikagebb.linktracker.bot.client.scrapper.model.ListLinksResponse;
import chikagebb.linktracker.bot.client.scrapper.model.RemoveLinkRequest;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScrapperClient {

    private final TgChatApi tgChatApi;
    private final LinksApi linksApi;

    public void registerChat(Long chatId) {
        tgChatApi.registerChat(chatId);
    }

    public void deleteChat(Long chatId) {
        tgChatApi.deleteChat(chatId);
    }

    public LinkResponse addLink(Long chatId, String url, List<String> tags) {
        var request = new AddLinkRequest();
        request.setLink(URI.create(url));
        request.setTags(tags);
        return linksApi.addLink(chatId, request);
    }

    public void removeLink(Long chatId, String url) {
        var request = new RemoveLinkRequest();
        request.setLink(URI.create(url));
        linksApi.removeLink(chatId, request);
    }

    public ListLinksResponse getLinks(Long chatId) {
        return linksApi.getLinks(chatId);
    }
}

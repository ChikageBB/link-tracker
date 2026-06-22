package chikagebb.linktracker.scrapper.service;

import chikagebb.linktracker.scrapper.model.AddLinkRequest;
import chikagebb.linktracker.scrapper.model.LinkResponse;
import chikagebb.linktracker.scrapper.model.ListLinksResponse;
import chikagebb.linktracker.scrapper.model.RemoveLinkRequest;
import chikagebb.linktracker.scrapper.repository.ChatRepository;
import chikagebb.linktracker.scrapper.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepository;
    private final ChatRepository chatRepository;

    @Transactional
    public LinkResponse add(Long chatId, AddLinkRequest addLinkRequest) {

        if (!chatRepository.exists(chatId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Чат не найден: " + chatId);
        }

        if (linkRepository.existByUrl(chatId, addLinkRequest.getLink())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Ссылка уже отслеживается: " + addLinkRequest.getLink()
            );
        }

        var link = linkRepository.save(chatId, addLinkRequest.getLink(), addLinkRequest.getTags());

        return new LinkResponse()
            .id(link.getId())
            .url(link.getUrl())
            .tags(link.getTags())
            .filters(List.of());
    }

    @Transactional
    public LinkResponse delete(Long chatId, RemoveLinkRequest removeLinkRequest) {
        if (!chatRepository.exists(chatId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Чат не найден: " + chatId);
        }

        var link = linkRepository
            .findByUrl(chatId, removeLinkRequest.getLink())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ссылка не найдена: " + removeLinkRequest.getLink()));

        linkRepository.delete(chatId, removeLinkRequest.getLink());

        return new LinkResponse()
            .id(link.getId())
            .url(link.getUrl())
            .tags(link.getTags())
            .filters(List.of());
    }

    @Transactional(readOnly = true)
    public ListLinksResponse getAll(Long chatId) {

        if (!chatRepository.exists(chatId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Чат не найден: " + chatId);
        }

        List<LinkResponse> links = linkRepository.findAll(chatId).stream()
            .map(l -> new LinkResponse()
                .id(l.getId())
                .url(l.getUrl())
                .tags(l.getTags())
                .filters(List.of()))
            .toList();

        return new ListLinksResponse().links(links).size(links.size());
    }
}

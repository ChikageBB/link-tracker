package chikagebb.linktracker.scrapper.repository.orm;

import chikagebb.linktracker.scrapper.dto.LinkDto;
import chikagebb.linktracker.scrapper.repository.LinkRepository;
import chikagebb.linktracker.scrapper.repository.orm.entity.Chat;
import chikagebb.linktracker.scrapper.repository.orm.entity.ChatLinkTag;
import chikagebb.linktracker.scrapper.repository.orm.entity.Link;
import chikagebb.linktracker.scrapper.repository.orm.entity.Tag;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnProperty(name = "app.access-type", havingValue = "ORM")
@RequiredArgsConstructor
public class OrmLinkRepository implements LinkRepository {

    private final LinkJpaRepository linkJpaRepository;
    private final ChatJpaRepository chatJpaRepository;
    private final TagJpaRepository tagJpaRepository;
    private final ChatLinkTagRepository chatLinkTagRepository;

    @Override
    @Transactional
    public LinkDto save(Long chatId, URI url, List<String> tags) {

        Link link = linkJpaRepository
                .findByUrl(url.toString())
                .orElseGet(() -> linkJpaRepository.save(new Link(url.toString())));

        Chat chat = chatJpaRepository.findById(chatId).orElseThrow();

        chat.getLinks().add(link);
        chatJpaRepository.save(chat);
        chatJpaRepository.flush();

        for (String tagName : tags) {
            Tag tag = tagJpaRepository.findByName(tagName).orElseGet(() -> tagJpaRepository.save(new Tag(tagName)));

            if (!chatLinkTagRepository.existsByChatIdAndLinkIdAndTagId(chatId, link.getId(), tag.getId())) {
                chatLinkTagRepository.save(new ChatLinkTag(chat, link, tag));
            }
        }

        return toDto(link, chatId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LinkDto> findAll(Long chatId, int page, int size) {
        return linkJpaRepository.findAllByChatId(chatId, PageRequest.of(page, size)).stream()
                .map(l -> toDto(l, chatId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existByUrl(Long chatId, URI url) {
        return linkJpaRepository.existsByChatIdAndUrl(chatId, url.toString());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LinkDto> findByUrl(Long chatId, URI url) {
        return linkJpaRepository.findByChatIdAndUrl(chatId, url.toString()).map(l -> toDto(l, chatId));
    }

    @Override
    @Transactional
    public boolean delete(Long chatId, URI url) {
        Chat chat = chatJpaRepository.findById(chatId).orElseThrow();
        return chat.getLinks().removeIf(l -> l.getUrl().equals(url.toString()));
    }

    @Override
    @Transactional
    public void deleteAllForChat(Long chatId) {
        chatLinkTagRepository.deleteByChatId(chatId);
        chatJpaRepository.findById(chatId).ifPresent(chat -> chat.getLinks().clear());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<LinkDto>> findAllLinkSubscriber(int page, int size) {
        List<Long> ids = linkJpaRepository.findAllLinkIds(PageRequest.of(page, size));

        if (ids.isEmpty()) return Map.of();

        return linkJpaRepository.findAllWithChatsByIds(ids).stream()
                .flatMap(link ->
                        link.getChats().stream().map(chat -> Map.entry(chat.getId(), toDto(link, chat.getId()))))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    @Override
    @Transactional
    public void updateLastCheckAt(Long linkId, OffsetDateTime time) {
        linkJpaRepository.updateLastCheck(linkId, time);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LinkDto> findOldestChecked(int page, int size) {
        return linkJpaRepository.findOldestChecked(PageRequest.of(page, size)).stream()
                .map(l -> toDto(l, null))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByDomain(String domainPattern) {
        return linkJpaRepository.countByUrlContaining(domainPattern);
    }

    private LinkDto toDto(Link link, Long chatId) {
        List<String> tags = chatLinkTagRepository.findTagNamesByChatIdAndLinkId(chatId, link.getId());
        LinkDto linkDto = new LinkDto(link.getId(), URI.create(link.getUrl()), tags);
        linkDto.setLastCheckAt(link.getLastCheckAt());
        return linkDto;
    }
}

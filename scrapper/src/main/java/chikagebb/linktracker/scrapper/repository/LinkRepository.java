package chikagebb.linktracker.scrapper.repository;

import chikagebb.linktracker.scrapper.dto.LinkDto;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LinkRepository {

    LinkDto save(Long chatId, URI url, List<String> tags);

    List<LinkDto> findAll(Long chatId, int page, int size);

    default List<LinkDto> findAll(Long chatId) {
        return findAll(chatId, 0, 100);
    }

    boolean existByUrl(Long chatId, URI url);

    Optional<LinkDto> findByUrl(Long chatId, URI url);

    void deleteAllForChat(Long chatId);

    Map<Long, List<LinkDto>> findAllLinkSubscriber(int page, int size);

    default Map<Long, List<LinkDto>> findAllLinkSubscriber() {
        return findAllLinkSubscriber(0, 100);
    }

    void updateLastCheckAt(Long linkId, OffsetDateTime time);

    List<LinkDto> findOldestChecked(int page, int size);

    long countByDomain(String domainPattern);

    boolean delete(Long chatId, URI url);
}

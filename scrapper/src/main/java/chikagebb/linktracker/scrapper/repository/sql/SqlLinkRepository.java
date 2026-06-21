package chikagebb.linktracker.scrapper.repository.sql;

import chikagebb.linktracker.scrapper.dto.LinkDto;
import chikagebb.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.access-type", havingValue = "SQL")
public class SqlLinkRepository implements LinkRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final LinkRowMapper ROW_MAPPER = new LinkRowMapper();

    private static final String INSERT_LINK = """
        INSERT INTO links(url)
        VALUES (?)
        ON CONFLICT (url) DO NOTHING
        """;

    private static final String SELECT_ID_BY_URL = """
        SELECT id FROM links WHERE url = ?
        """;

    private static final String INSERT_CHAT_LINKS = """
        INSERT INTO chat_links (chat_id, link_id) VALUES (?,?)
        """;

    private static final String INSERT_TAGS = """
        INSERT INTO tags (name)
        VALUES (?)
        ON CONFLICT (name) DO NOTHING
        """;

    private static final String INSERT_CHAT_LINK_TAGS = """
        INSERT INTO chat_link_tags (chat_id, link_id, tag_id)
        SELECT ?, ?, id FROM tags WHERE name = ?
        """;

    private static final String FIND_ALL = """
        SELECT l.id, l.url, l.last_check_at, ARRAY_AGG(t.name) FILTER ( WHERE t.name IS NOT NULL ) AS tags
        FROM links l
        JOIN chat_links cl ON cl.chat_id = ? AND cl.link_id = l.id
        LEFT JOIN chat_link_tags clt ON clt.chat_id = ? AND clt.link_id = l.id
        LEFT JOIN tags t ON t.id = clt.tag_id
        GROUP BY l.id, l.url, l.last_check_at
        LIMIT ? OFFSET ?
        """;

    private static final String EXIST_BY_URL = """
        SELECT COUNT(*)
        FROM links l
        JOIN chat_links cl ON cl.link_id = l.id
        WHERE cl.chat_id = ? AND l.url = ?
        """;

    private static final String FIND_BY_URL = """
        SELECT l.id, l.url, l.last_check_at, ARRAY_AGG(t.name) FILTER ( WHERE t.name IS NOT NULL ) as tags
        FROM links l
        JOIN chat_links cl ON cl.link_id = l.id AND cl.chat_id = ?
        LEFT JOIN chat_link_tags clt ON clt.chat_id = cl.chat_id AND clt.link_id = l.id
        LEFT JOIN tags t ON t.id = clt.tag_id
        WHERE l.url = ?
        GROUP BY l.id, l.url, l.last_check_at
        """;

    private static final String DELETE_FOR_ALL_CHAT = """
            DELETE FROM chat_links WHERE chat_id = ?
        """;

    private static final String FIND_ALL_SUBSCRIBERS = """
        SELECT l.id, l.url, l.last_check_at, cl.chat_id,
               ARRAY_AGG(t.name) FILTER ( WHERE t.name IS NOT NULL ) AS tags
        FROM links l
        JOIN chat_links cl ON cl.link_id = l.id
        LEFT JOIN chat_link_tags clt ON clt.chat_id = cl.chat_id AND clt.link_id = l.id
        LEFT JOIN tags t ON t.id = clt.tag_id
        GROUP BY l.id, l.url, l.last_check_at, cl.chat_id
        LIMIT ? OFFSET ?
        """;

    private static final String UPDATE_LAST_CHECK_AT = """
        UPDATE links SET last_check_at = ? WHERE id = ?
        """;

    private static final String FIND_OLDEST_CHECKED = """
        SELECT l.id, l.url, l.last_check_at,
               ARRAY_AGG(t.name) FILTER (WHERE t.name IS NOT NULL) as tags
        FROM links l
        LEFT JOIN chat_links cl on cl.link_id = l.id
        LEFT JOIN chat_link_tags clt ON clt.link_id = l.id AND clt.chat_id = cl.chat_id
        LEFT JOIN tags t ON t.id = clt.tag_id
        GROUP BY l.id, l.url, l.last_check_at
        ORDER BY l.last_check_at ASC NULLS FIRST
        LIMIT ? OFFSET ?
        """;

    private static final String DELETE = """
        DELETE FROM chat_links
        WHERE chat_id = ? AND
              link_id = (SELECT id FROM links WHERE url = ?)
        """;

    private static final String COUNT_BY_DOMAIN = """
        SELECT COUNT(*) FROM links WHERE url LIKE ?
        """;

    @Override
    public LinkDto save(Long chatId, URI url, List<String> tags) {
        jdbcTemplate.update(INSERT_LINK, url.toString());

        Long linkId = jdbcTemplate.queryForObject(SELECT_ID_BY_URL, Long.class, url.toString());

        jdbcTemplate.update(INSERT_CHAT_LINKS, chatId, linkId);

        for (String tag : tags) {
            jdbcTemplate.update(INSERT_TAGS, tag);

            jdbcTemplate.update(INSERT_CHAT_LINK_TAGS, chatId, linkId, tag);
        }

        return findByUrl(chatId, url).orElseThrow();
    }

    @Override
    public List<LinkDto> findAll(Long chatId, int page, int size) {
        return jdbcTemplate.query(FIND_ALL, ROW_MAPPER, chatId, chatId, size, page * size);
    }

    @Override
    public boolean existByUrl(Long chatId, URI url) {
        Integer count = jdbcTemplate.queryForObject(EXIST_BY_URL, Integer.class, chatId, url.toString());

        return count != null && count != 0;
    }

    @Override
    public Optional<LinkDto> findByUrl(Long chatId, URI url) {
        List<LinkDto> result = jdbcTemplate.query(FIND_BY_URL, ROW_MAPPER, chatId, url.toString());

        return result.stream().findFirst();
    }

    @Override
    @Transactional
    public boolean delete(Long chatId, URI url) {
       int rowsDeleted = jdbcTemplate.update(DELETE, chatId, url.toString());

       return rowsDeleted > 0;
    }

    @Override
    public void deleteAllForChat(Long chatId) {
        jdbcTemplate.update(DELETE_FOR_ALL_CHAT, chatId);
    }

    @Override
    public Map<Long, List<LinkDto>> findAllLinkSubscriber(int page, int size) {
        Map<Long, List<LinkDto>> result = new LinkedHashMap<>();

        jdbcTemplate.query(
                FIND_ALL_SUBSCRIBERS,
                rs -> {
                    Long chatId = rs.getLong("chat_id");
                    LinkDto link = ROW_MAPPER.mapRow(rs, 0);
                    result.computeIfAbsent(chatId, k -> new ArrayList<>()).add(link);
                },
                size,
                page * size);
        return result;
    }

    @Override
    public void updateLastCheckAt(Long linkId, OffsetDateTime time) {
        jdbcTemplate.update(UPDATE_LAST_CHECK_AT, time, linkId);
    }

    @Override
    public List<LinkDto> findOldestChecked(int page, int size) {
        return jdbcTemplate.query(FIND_OLDEST_CHECKED, ROW_MAPPER, size, page * size);
    }

    @Override
    public long countByDomain(String domainPattern) {
        Long count = jdbcTemplate.queryForObject(COUNT_BY_DOMAIN, Long.class, "%" + domainPattern + "%");
        return count == null ? 0 : count;
    }
}

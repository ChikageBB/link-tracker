package chikagebb.linktracker.scrapper.repository.jooq;

import chikagebb.linktracker.scrapper.dto.LinkDto;
import chikagebb.linktracker.scrapper.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.jooq.impl.DSL.arrayAgg;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.selectOne;
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.DSL.val;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.access-type", havingValue = "JOOQ")
public class JooqLinkRepository implements LinkRepository {

    private final DSLContext dslContext;

    @Override
    @Transactional
    public LinkDto save(Long chatId, URI url, List<String> tags) {

        dslContext.insertInto(table("links"))
            .columns(field("url"))
            .values(url.toString())
            .onConflictDoNothing()
            .execute();

        Long linkId = dslContext
            .select(field("id", Long.class))
            .from(table("links"))
            .where(field("url").eq(url.toString()))
            .fetchOne(0, Long.class);

        dslContext.insertInto(table("chat_links"))
            .columns(field("chat_id"), field("link_id"))
            .values(chatId, linkId)
            .execute();

        for (String tag : tags) {
            dslContext.insertInto(table("tags"))
                .columns(field("name"))
                .values(tag)
                .onConflictDoNothing()
                .execute();

            dslContext.insertInto(table("chat_link_tags"))
                .columns(field("chat_id", Long.class), field("link_id", Long.class), field("tag_id", Long.class))
                .select(
                    select(val(chatId), val(linkId), field("id", Long.class))
                        .from(table("tags"))
                        .where(field("name").eq(tag))
                )
                .execute();
        }

        return findByUrl(chatId, url).orElseThrow();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LinkDto> findAll(Long chatId, int page, int size) {
        Field<String[]> tags = arrayAgg(field("t.name", String.class))
            .filterWhere(field("t.name").isNotNull())
            .as("tags");

        return dslContext
            .select(field("l.id", Long.class).as("id"), field("l.url", String.class).as("url"),
                field("l.last_check_at", OffsetDateTime.class).as("last_check_at"), tags)
            .from(table("links").as("l"))
            .join(table("chat_links").as("cl"))
                .on(field("cl.link_id").eq(field("l.id"))).and(field("cl.chat_id").eq(chatId))
            .leftJoin(table("chat_link_tags").as("clt"))
                .on(field("clt.chat_id").eq(field("cl.chat_id"))).and(field("clt.link_id").eq(field("l.id")))
            .leftJoin(table("tags").as("t")).on(field("t.id").eq(field("clt.tag_id")))
            .groupBy(field("l.id"), field("l.url"), field("l.last_check_at"))
            .limit(size).offset(page * size).fetch(this::toDto);

    }

    @Override
    @Transactional(readOnly = true)
    public boolean existByUrl(Long chatId, URI url) {
        return dslContext.fetchExists(
            selectOne().from(table("chat_links").as("cl"))
                .join(table("links").as("l")).on(field("cl.link_id").eq(field("l.id")))
                .where(field("cl.chat_id").eq(chatId))
                .and(field("l.url").eq(url.toString()))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LinkDto> findByUrl(Long chatId, URI url) {
        Field<String[]> tags = arrayAgg(field("t.name", String.class))
            .filterWhere(field("t.name").isNotNull())
            .as("tags");

        return dslContext
            .select(field("l.id", Long.class).as("id"), field("l.url", String.class).as("url"),
                field("l.last_check_at", OffsetDateTime.class).as("last_check_at"), tags)
            .from(table("links").as("l"))
            .join(table("chat_links").as("cl")).on(field("cl.link_id").eq(field("l.id")))
            .and(field("cl.chat_id").eq(chatId))
            .leftJoin(table("chat_link_tags").as("clt")).on(field("clt.chat_id").eq(field("cl.chat_id")))
            .and(field("clt.link_id").eq(field("l.id")))
            .leftJoin(table("tags").as("t")).on(field("t.id").eq(field("clt.tag_id")))
            .where(field("l.url").eq(url.toString()))
            .groupBy(field("l.id"), field("l.url"), field("l.last_check_at"))
            .fetchOptional(this::toDto);
    }

    @Override
    @Transactional
    public boolean delete(Long chatId, URI url) {
        int rowsDeleted = dslContext.deleteFrom(table("chat_links"))
            .where(field("chat_id").eq(chatId))
            .and(
                field("link_id").eq(
                    dslContext.select(field("id", Long.class))
                        .from(table("links"))
                        .where(field("url").eq(url.toString()))
                )
            ).execute();

        return rowsDeleted > 0;
    }

    @Override
    @Transactional
    public void deleteAllForChat(Long chatId) {
        dslContext.deleteFrom(table("chat_links"))
            .where(field("chat_id").eq(chatId))
            .execute();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<LinkDto>> findAllLinkSubscriber(int page, int size) {
        Field<String[]> tags = arrayAgg(field("t.name", String.class))
            .filterWhere(field("t.name").isNotNull())
            .coerce(String[].class)
            .as("tags");

        return dslContext
            .select(
                field("cl.chat_id", Long.class).as("chat_id"),
                field("l.id", Long.class).as("id"),
                field("l.url", String.class).as("url"),
                field("l.last_check_at", OffsetDateTime.class).as("last_check_at"),
                tags
            )
            .from(table("links").as("l"))
            .join(table("chat_links").as("cl")).on(field("cl.link_id").eq(field("l.id")))
            .leftJoin(table("chat_link_tags").as("clt"))
                .on(field("clt.chat_id").eq(field("cl.chat_id")))
                .and(field("clt.link_id").eq(field("l.id")))
            .leftJoin(table("tags").as("t")).on(field("t.id").eq(field("clt.tag_id")))
            .groupBy(
                field("l.id"),
                field("l.url"),
                field("l.last_check_at"),
                field("cl.chat_id")
            )
            .limit(size)
            .offset(page * size)
            .fetchGroups(
                r -> r.get("chat_id", Long.class),
                this::toDto
            );
    }

    @Override
    @Transactional
    public void updateLastCheckAt(Long linkId, OffsetDateTime time) {
        dslContext.update(table("links"))
            .set(field("last_check_at"), time)
            .where(field("id").eq(linkId)).execute();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LinkDto> findOldestChecked(int page, int size) {
        Field<String[]> tags = arrayAgg(field("t.name", String.class))
            .filterWhere(field("t.name").isNotNull())
            .as("tags");

        return dslContext
            .select(field("l.id", Long.class).as("id"), field("l.url", String.class).as("url"),
                field("l.last_check_at", OffsetDateTime.class).as("last_check_at"), tags)
            .from(table("links").as("l"))
            .join(table("chat_links").as("cl")).on(field("cl.link_id").eq(field("l.id")))
            .leftJoin(table("chat_link_tags").as("clt")).on(field("clt.chat_id").eq(field("cl.chat_id")))
            .and(field("clt.link_id").eq(field("l.id")))
            .leftJoin(table("tags").as("t")).on(field("t.id").eq(field("clt.tag_id")))
            .groupBy(field("l.id", Long.class), field("l.url", String.class),
                field("l.last_check_at", OffsetDateTime.class))
            .orderBy(field("l.last_check_at", OffsetDateTime.class).asc().nullsFirst())
            .limit(size).offset(page * size).fetch(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByDomain(String domainPattern) {
        Long count = dslContext.selectCount()
            .from(table("links"))
            .where(field("url").like("%" + domainPattern + "%"))
            .fetchOne(0, Long.class);

        return count == null ? 0 : count;
    }

    private LinkDto toDto(Record r) {
        String[] tags = r.get("tags", String[].class);
        LinkDto dto = new LinkDto(
            r.get("id", Long.class),
            URI.create(r.get("url", String.class)),
            tags == null ? List.of() : Arrays.asList(tags)
        );

        dto.setLastCheckAt(r.get("last_check_at" ,OffsetDateTime.class));

        return dto;
    }
}

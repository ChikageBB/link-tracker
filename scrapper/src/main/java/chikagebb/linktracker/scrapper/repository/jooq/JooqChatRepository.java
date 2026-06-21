package chikagebb.linktracker.scrapper.repository.jooq;

import chikagebb.linktracker.scrapper.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.selectOne;
import static org.jooq.impl.DSL.table;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.access-type", havingValue = "JOOQ")
public class JooqChatRepository implements ChatRepository {

    private final DSLContext dslContext;

    @Override
    @Transactional
    public void save(Long chatId) {
        dslContext.insertInto(table("chats"))
            .columns(field("id"))
            .values(chatId)
            .execute();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(Long chatId) {
        return dslContext.fetchExists(
            selectOne()
                .from(table("chats"))
                .where(field("id").eq(chatId))
        );
    }

    @Override
    @Transactional
    public void remove(Long chatId) {
        dslContext.deleteFrom(table("chats"))
            .where(field("id").eq(chatId))
            .execute();
    }
}

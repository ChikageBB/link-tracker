package chikagebb.linktracker.scrapper.repository.sql;

import chikagebb.linktracker.scrapper.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.access-type", havingValue = "SQL")
public class SqlChatRepository implements ChatRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String INSERT_CHATS = "INSERT INTO chats (id) VALUES (?)";
    private static final String EXISTS = "SELECT COUNT(*) FROM chats WHERE id = ?";
    private static final String DELETE = "DELETE FROM chats WHERE id = ?";

    @Override
    public void save(Long chatId) {
        jdbcTemplate.update(INSERT_CHATS, chatId);
    }

    @Override
    public boolean exists(Long chatId) {
        Integer count = jdbcTemplate.queryForObject(EXISTS, Integer.class, chatId);

        return count != null && count != 0;
    }

    @Override
    public void remove(Long chatId) {
        jdbcTemplate.update(DELETE, chatId);
    }
}

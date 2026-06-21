package chikagebb.linktracker.scrapper.repository;


import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
public abstract class AbstractLinkRepositoryTest extends PostgresContainerBase {

    private static final Long CHAT_ID = 1L;
    private static final String URL = "https://github.com/a/b";

    @Autowired
    protected LinkRepository linkRepository;

    @Autowired
    protected ChatRepository chatRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        linkRepository.deleteAllForChat(CHAT_ID);
        chatRepository.remove(CHAT_ID);
        entityManager.clear();
        chatRepository.save(CHAT_ID);
    }

    @Test
    void addLink_shouldSaveToDB() {
        var link = linkRepository.save(CHAT_ID, URI.create(URL), List.of());
        assertThat(link.getId()).isNotNull();
        assertThat(linkRepository.existByUrl(CHAT_ID, URI.create(URL))).isTrue();
    }

    @Test
    void deleteLink_shouldRemoveFromDB() {
        linkRepository.save(CHAT_ID, URI.create(URL), List.of());
        linkRepository.delete(CHAT_ID, URI.create(URL));
        assertThat(linkRepository.existByUrl(CHAT_ID, URI.create(URL))).isFalse();
    }

    @Test
    void addDuplicateLink_shouldThrow() {
        linkRepository.save(CHAT_ID, URI.create(URL), List.of());
        assertThatThrownBy(() -> linkRepository.save(CHAT_ID, URI.create(URL), List.of()))
            .isInstanceOf(Exception.class);
    }
}

package chikagebb.linktracker.scrapper.repository.orm;

import chikagebb.linktracker.scrapper.repository.orm.entity.Link;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LinkJpaRepository extends JpaRepository<Link, Long> {

    Optional<Link> findByUrl(String url);

    @Query("""
        SELECT COUNT(l) > 0
        FROM Chat c
        JOIN c.links l
        WHERE c.id = :chatId AND l.url = :url
    """)
    boolean existsByChatIdAndUrl(@Param("chatId") Long chatId, @Param("url") String url);

    @Query("""
        SELECT l
        FROM Chat c
        JOIN c.links l
        WHERE c.id = :chatId
        """)
    List<Link> findAllByChatId(@Param("chatId") Long chatId, Pageable pageable);

    @Query("""
        SELECT l
        FROM Chat c
        JOIN c.links l
        WHERE c.id = :chatId AND l.url = :url
    """)
    Optional<Link> findByChatIdAndUrl(@Param("chatId") Long chatId, @Param("url") String url);

    @Query("""
    SELECT DISTINCT l.id FROM Link l JOIN l.chats
    """)
    List<Long> findAllLinkIds(Pageable pageable);

    @Modifying
    @Query("""
        UPDATE Link l SET l.lastCheckAt = :time WHERE l.id = :linkId
    """)
    void updateLastCheck(@Param("linkId") Long linkId, @Param("time") OffsetDateTime time);

    @Query("""
        SELECT l FROM Link l ORDER BY l.lastCheckAt ASC NULLS FIRST
        """)
    List<Link> findOldestChecked(Pageable pageable);

    @Query("""
        SELECT DISTINCT l
        FROM Link l
        JOIN FETCH l.chats
        WHERE l.id IN :ids
        """)
    List<Link> findAllWithChatsByIds(@Param("ids") List<Long> ids);

    long countByUrlContaining(String urlPart);
}

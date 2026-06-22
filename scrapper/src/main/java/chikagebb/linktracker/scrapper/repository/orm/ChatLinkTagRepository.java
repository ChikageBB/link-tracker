package chikagebb.linktracker.scrapper.repository.orm;

import chikagebb.linktracker.scrapper.repository.orm.entity.ChatLinkTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatLinkTagRepository extends JpaRepository<ChatLinkTag, Long> {

    @Query("""
        SELECT clt
        FROM ChatLinkTag clt
        WHERE clt.chat.id = :chatId and clt.link.id = :linkId
    """)
    List<ChatLinkTag> findByChatIdAndLinkId(@Param("chatId") Long chatId, @Param("linkId") Long linkId);

    @Query("""
        SELECT clt.tag.name
        FROM ChatLinkTag clt
        WHERE clt.chat.id = :chatId AND clt.link.id = :linkId
    """)
    List<String> findTagNamesByChatIdAndLinkId(@Param("chatId") Long chatId, @Param("linkId") Long linkId);

    @Modifying
    @Query("DELETE FROM ChatLinkTag clt WHERE clt.chat.id = :chatId")
    void deleteByChatId(@Param("chatId") Long chatId);

    boolean existsByChatIdAndLinkId(Long chatId, Long linkId);

    boolean existsByChatIdAndLinkIdAndTagId(Long chatId, Long linkId, Long tagId);
}

package chikagebb.linktracker.scrapper.repository.orm.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
public class ChatLinkTagId implements Serializable {

    private Long chat;
    private Long link;
    private Long tag;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ChatLinkTagId that)) return false;
        return Objects.equals(chat, that.chat) && Objects.equals(link, that.link) && Objects.equals(tag, that.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chat, link, tag);
    }
}

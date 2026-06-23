package chikagebb.linktracker.scrapper.dto;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinkDto {

    private Long id;
    private URI url;
    private List<String> tags;
    private OffsetDateTime lastCheckAt;

    public LinkDto(Long id, URI url, List<String> tags) {
        this.id = id;
        this.url = url;
        this.tags = tags;
        this.lastCheckAt = OffsetDateTime.now();
    }
}

package chikagebb.linktracker.scrapper.repository.sql;

import chikagebb.linktracker.scrapper.dto.LinkDto;
import java.net.URI;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;

public class LinkRowMapper implements RowMapper<LinkDto> {

    @Override
    public LinkDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        Long id = rs.getLong("id");
        URI url = URI.create(rs.getString("url"));

        Array tagsArray = rs.getArray("tags");
        List<String> tags = tagsArray != null ? Arrays.asList((String[]) tagsArray.getArray()) : List.of();

        LinkDto linkDto = new LinkDto(id, url, tags);
        linkDto.setLastCheckAt(rs.getObject("last_checked_at", OffsetDateTime.class));

        return linkDto;
    }
}

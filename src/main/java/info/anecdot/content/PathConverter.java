package info.anecdot.content;

import org.springframework.util.StringUtils;

import javax.persistence.AttributeConverter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * @author Stephan Grundner
 */
public class PathConverter implements AttributeConverter<Path, String> {

    @Override
    public String convertToDatabaseColumn(Path path) {
        return Optional.ofNullable(path)
                .map(Path::toString)
                .orElse(null);
    }

    @Override
    public Path convertToEntityAttribute(String value) {
        if (StringUtils.hasText(value)) {
            return Paths.get(value);
        }

        return null;
    }
}

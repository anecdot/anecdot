package info.anecdot.io;

import org.springframework.util.StringUtils;

import javax.persistence.AttributeConverter;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Stephan Grundner
 */
public class PathToStringConverter implements AttributeConverter<Path, String> {

    @Override
    public String convertToDatabaseColumn(Path path) {
        if (path != null) {
            return path.toString();
        }

        return null;
    }

    @Override
    public Path convertToEntityAttribute(String value) {
        if (StringUtils.hasText(value)) {
            return Paths.get(value);
        }

        return null;
    }
}

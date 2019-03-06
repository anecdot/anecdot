package info.anecdot.content;

import javax.persistence.Entity;
import javax.persistence.Lob;

/**
 * @author Stephan Grundner
 */
@Entity
public class Text extends Knot {

    @Lob
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

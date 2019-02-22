package info.anecdot.content;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * @author Stephan Grundner
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"site_id", "uri"}))
@DiscriminatorValue("item")
public class Item extends Knot {

    @ManyToOne(optional = false)
    @JoinColumn(name = "site_id")
    private Site site;
    private String uri;

    private String type;

    private LocalDateTime lastModified;

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;

        if (!site.getItems().contains(this)) {
            site.getItems().add(this);
        }
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }
}

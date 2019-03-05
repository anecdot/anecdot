package info.anecdot.content;

import javax.persistence.*;
import java.net.URI;
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

    @Convert(converter = URIConverter.class)
    private URI uri;

    private String type;

    private LocalDateTime lastModified;
    private String syncId;

    private String description;

    @Convert(converter = URIConverter.class)
    private URI image;

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
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

    public String getSyncId() {
        return syncId;
    }

    public void setSyncId(String syncId) {
        this.syncId = syncId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public URI getImage() {
        return image;
    }

    public void setImage(URI image) {
        this.image = image;
    }
}

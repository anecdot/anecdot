package info.anecdot.content;

import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Stephan Grundner
 */
@Entity
@SecondaryTable(name = "item")
public class Item extends Knot {

    @OneToOne
    @JoinColumn(name = "asset_id", table = "item")
    private Asset asset;

    @Column(table = "item")
    private String type;

    @Column(table = "item")
    private String title;

    @Column(table = "item")
    private String description;

    @ElementCollection()
    @CollectionTable(name = "tag")
    @Column(name = "value", table = "item")
    private final Set<String> tags = new HashSet<>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getTags() {
        return tags;
    }

    public URI getUri() {
        return URI.create(StringUtils.removeEnd(asset.getPath(), ".xml"));
    }
}

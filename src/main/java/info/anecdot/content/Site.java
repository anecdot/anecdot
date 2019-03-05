package info.anecdot.content;

import info.anecdot.io.PathToStringConverter;
import org.apache.commons.io.FilenameUtils;
import org.springframework.util.StringUtils;

import javax.persistence.*;
import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;

/**
 * @author Stephan Grundner
 */
@Entity
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;

    private boolean busy;

    private String home;
    private Locale locale;

    @Convert(converter = PathToStringConverter.class)
    private Path contentDirectory;

    @Convert(converter = PathToStringConverter.class)
    private Path themeDirectory;

//    @OneToMany(mappedBy = "site", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
//    private final List<Item> items = new ArrayList<>();

    private String syncId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public String getHome() {
        return home;
    }

    public void setHome(String home) {
        this.home = home;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Path getContentDirectory() {
        return contentDirectory;
    }

    public void setContentDirectory(Path contentDirectory) {
        this.contentDirectory = contentDirectory;
    }

    public Path getThemeDirectory() {
        return themeDirectory;
    }

    public void setThemeDirectory(Path themeDirectory) {
        this.themeDirectory = themeDirectory;
    }

//    public List<Item> getItems() {
//        return Collections.unmodifiableList(items);
//    }
//
//    public boolean addItem(Item item) {
//        if (items.add(item)) {
//            item.site = this;
//
//            return true;
//        }
//
//        return false;
//    }

    public String getSyncId() {
        return syncId;
    }

    public void setSyncId(String syncId) {
        this.syncId = syncId;
    }

    public URI toURI(Path file) {
        String uri = contentDirectory.relativize(file).toString();
        uri = FilenameUtils.removeExtension(uri);
        if (!StringUtils.startsWithIgnoreCase(uri, "/")) {
            uri = "/" + uri;
        }

        return URI.create(uri);
    }
}

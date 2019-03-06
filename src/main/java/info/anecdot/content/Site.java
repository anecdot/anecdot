package info.anecdot.content;

import javax.persistence.*;
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
    private String host;

    private Locale locale;
    private String home;

    @Convert(converter = PathConverter.class)
    private Path theme;

    @Embedded
    private Webdav webdav;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getHome() {
        return home;
    }

    public void setHome(String home) {
        this.home = home;
    }

    public Path getTheme() {
        return theme;
    }

    public void setTheme(Path theme) {
        this.theme = theme;
    }

    public Webdav getWebdav() {
        return webdav;
    }

    public void setWebdav(Webdav webdav) {
        this.webdav = webdav;
    }
}

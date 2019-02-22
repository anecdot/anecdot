package info.anecdot.settings;

import info.anecdot.security.Permission;

import java.util.List;
import java.util.Locale;

/**
 * @author Stephan Grundner
 */
public class Settings {

    private final String path;
    private Locale locale;

    private List<String> ignorePatterns;
    private List<Permission> permissions;

    public String getPath() {
        return path;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public List<String> getIgnorePatterns() {
        return ignorePatterns;
    }

    public void setIgnorePatterns(List<String> ignorePatterns) {
        this.ignorePatterns = ignorePatterns;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }

    public Settings(String path) {
        this.path = path;
    }
}

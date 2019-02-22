package info.anecdot.security;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stephan Grundner
 */
public class Permission {

    public enum Kind {
        ALLOW,
        DENY
    }

    private final Kind kind;
    private String pattern;
    private List<String> roles;
    private List<String> users;

    public Kind getKind() {
        return kind;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public List<String> getRoles() {
        if (roles == null) {
            roles = new ArrayList<>();
        }

        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getUsers() {
        if (users == null) {
            users = new ArrayList<>();
        }

        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public Permission(Kind kind, String pattern, List<String> roles) {
        this.kind = kind;
        this.pattern = pattern;
        this.roles = roles;
    }

    public Permission(Kind kind) {
        this.kind = kind;
    }
}

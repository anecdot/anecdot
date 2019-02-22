package info.anecdot.settings;

import info.anecdot.content.Site;
import info.anecdot.security.Permission;
import info.anecdot.xml.DomAndXPathSupport;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;

/**
 * @author Stephan Grundner
 */
@Service
public class SettingsService implements DomAndXPathSupport {

    @Autowired
    private ApplicationContext applicationContext;

    private final Map<String, Settings> settingsByUri = new ConcurrentSkipListMap<>();

    public Collection<Settings> getAllSettings() {
        return Collections.unmodifiableCollection(settingsByUri.values());
    }

    private void eachSegmentForUri(String uri, Consumer<String> consumer) {
        if (uri.endsWith("/") && uri.length() > 1) {
            uri = uri.substring(1, uri.length() - 1);
        }

        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }

        for (int i = 0; i < uri.length(); i++) {
            char c = uri.charAt(i);
            if (c == '/' || i == uri.length() - 1) {
                String segment = uri.substring(0, i+1);
                consumer.accept(segment);
            }
        }
    }

    public void eachSettingsForUri(String uri, Consumer<Settings> consumer) {
        eachSegmentForUri(uri, segment -> {
            Settings settings = settingsByUri.get(segment);
            if (settings != null) {
                consumer.accept(settings);
            }
        });
    }

    public Set<String> getUris() {
        return Collections.unmodifiableSet(settingsByUri.keySet());
    }

    public Settings getSettings(String uri) {
        return settingsByUri.get(uri);
    }

    private void applyLocale(Node settingsNode, Settings settings) {
        nodes("/settings/locale", settingsNode).forEach(node -> {
            String languageTag = node.getTextContent();
            Locale locale = Locale.forLanguageTag(languageTag);
            settings.setLocale(locale);
        });
    }

    private void applySecurity(Node document, final Settings settings) {
        List<Permission> permissions = new ArrayList<>();

        nodes("/settings/security/*", document).forEach(node -> {
            Permission.Kind kind;
            switch (node.getNodeName()) {
                case "allow":
                    kind = Permission.Kind.ALLOW;
                    break;
                case "deny":
                    kind = Permission.Kind.DENY;
                    break;
                default:
                    throw new IllegalStateException();
            }

            Permission permission = new Permission(kind);
            String path = attribute("path", node);
            permission.setPattern(path);

            nodes("user", node)
                    .map(Node::getTextContent)
                    .forEach(permission.getUsers()::add);

            nodes("role", node)
                    .map(Node::getTextContent)
                    .map(it -> "ROLE_" + it)
                    .forEach(permission.getRoles()::add);

            permissions.add(permission);
        });

        settings.setPermissions(permissions);
    }

    public void reloadSettings(Site site, Path file)  {
        String path = site.getContentDirectory().relativize(file).toString();
        path = FilenameUtils.removeExtension(path);
        path = FilenameUtils.removeExtension(path);
        if (!StringUtils.startsWithIgnoreCase(path, "/")) {
            path = "/" + path;
        }

        Settings settings = new Settings(path);

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

            dbf.setIgnoringElementContentWhitespace(true);
            dbf.setNamespaceAware(true);
//            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
//            Resource schemaResource = applicationContext.getResource("classpath:/schema/settings.xsd");
//            try (InputStream inputStream = schemaResource.getInputStream()) {
//                Schema schema = sf.newSchema(new StreamSource(inputStream));
//                dbf.setSchema(schema);
//
//            } catch (SAXException e) {
//                throw new RuntimeException(e);
//            }

            DocumentBuilder parser = dbf.newDocumentBuilder();

            try (InputStream inputStream = Files.newInputStream(file)) {
                Document document = parser.parse(inputStream);

                applyLocale(document, settings);
                applySecurity(document, settings);
            }

            settingsByUri.put(path, settings);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public XPath getXPath() {
        return XPathFactory.newInstance().newXPath();
    }
}

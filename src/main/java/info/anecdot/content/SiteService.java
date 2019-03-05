package info.anecdot.content;

import info.anecdot.io.PathWatcher;
import info.anecdot.settings.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * @author Stephan Grundner
 */
@Service
public class SiteService {

    private static final Logger LOG = LoggerFactory.getLogger(SiteService.class);

    private static List<String> getProperties(PropertyResolver propertyResolver, String key, List<String> defaultValues) {
        class StringArrayList extends ArrayList<String> {
            private StringArrayList(Collection<? extends String> c) {
                super(c);
            }

            public StringArrayList() { }
        }

        return propertyResolver.getProperty(key, StringArrayList.class, new StringArrayList(defaultValues));
    }

    private static List<String> getProperties(PropertyResolver propertyResolver, String key) {
        return getProperties(propertyResolver, key, Collections.emptyList());
    }

    private final Map<String, PathWatcher> watcherBySiteName = new HashMap<>();

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private TaskExecutor taskExecutor;

    public List<Site> getAllSites() {
        return siteRepository.findAll();
    }

    public PathWatcher getWatcher(Site site) {
        return watcherBySiteName.get(site.getName());
    }

    public Site findSiteByName(String name) {
        return siteRepository.findByName(name);
    }

    private Site findOrCreateSiteByName(String name) {
        Site site = findSiteByName(name);
        if (site == null) {
            site = new Site();
            site.setName(name);
//            site = saveSite(site);
        }

        return site;
    }

    public Site findSiteByRequest(HttpServletRequest request) {
        return findSiteByName(request.getServerName());
    }

    private void reload(Site site, Path file) {
        try {
            String fileName = file.getFileName().toString();

            if (".settings.xml".equals(fileName)) {
                settingsService.reloadSettings(site, file);

                return;
            }

            if (!fileName.endsWith(".xml")) {
                LOG.info("Ignoring " + file);

                return;
            }

            URI uri = site.toURI(file);
            Item item = itemService.findItemBySiteAndURI(site, uri);
            if (item != null) {
                BasicFileAttributes fileAttributes = Files.readAttributes(file, BasicFileAttributes.class);
                LocalDateTime lastModified = LocalDateTime.ofInstant(
                        fileAttributes.lastModifiedTime().toInstant(),
                        ZoneId.systemDefault());
                if (lastModified.isAfter(item.getLastModified())) {
                    itemService.loadItem(site, file);
                    LOG.info("Reloaded " + file);
                } else {
                    item.setSyncId(site.getSyncId());
                    item = itemService.saveItem(item);
                }
            } else {
                itemService.loadItem(site, file);
                LOG.info("Loaded " + file);
            }
        } catch (Exception e) {
            LOG.error("Error reloading file " + file, e);
//            throw new RuntimeException(e);
        }
    }

    private Site saveSite(Site site) {
        return siteRepository.save(site);
    }

    private void deleteSitesNotIn(List<Site> sites) {
        siteRepository.findAllNotIn(sites)
                .forEach(siteRepository::delete);
    }

    private void deleteItemByFile(Site site, Path file) {
        URI uri = site.toURI(file);
        itemService.deleteItemBySiteAndURI(site, uri);
    }

    public void config() throws IOException {
        Environment environment = applicationContext.getEnvironment();
        List<String> keys = getProperties(environment, "anecdot.sites");

        List<Site> sites = new ArrayList<>();

        for (String key : keys) {
            String prefix = String.format("anecdot.site.%s", key);
            String name = environment.getProperty(prefix + ".host");

            Cache cache = cacheManager.getCache("sites");
            cache.evict(name);

            final Site site = findOrCreateSiteByName(name);
            site.setBusy(true);
            site.setSyncId(UUID.randomUUID().toString());

//            List<String> names = getProperties(propertyResolver, prefix + ".aliases");
//            site.getAliases().addAll(names);

            String content = environment.getProperty(prefix + ".base");
            if (StringUtils.hasText(content)) {
                site.setContentDirectory(Paths.get(content));
            }

            String theme = environment.getProperty(prefix + ".theme");
            if (StringUtils.hasText(theme)) {
                site.setThemeDirectory(Paths.get(theme));
            }

            String home = environment.getProperty(prefix + ".home", "/home");
            site.setHome(home);

            Locale locale = environment.getProperty(prefix + ".locale", Locale.class);
            site.setLocale(locale);

            Site saved = saveSite(site);
            sites.add(saved);
            cache.put(name, saved);

            PathWatcher watcher = new PathWatcher(site.getContentDirectory());
            watcherBySiteName.put(site.getName(), watcher);

            watcher.setHandler(new PathWatcher.AbstractWatchHandler() {
                @Override
                public void initialized() {
                    site.setBusy(false);
                    saveSite(site);
                    itemService.deleteAllObsoleteItems(site);
                }

                @Override
                public void visited(Path path) {
                    if (Files.isRegularFile(path)) {
                        reload(site, path);
                    }
                }

                @Override
                public void created(Path path) {
                    if (Files.isRegularFile(path)) {
                        reload(site, path);
                    }
                }

                @Override
                public void modified(Path path) {
                    if (Files.isRegularFile(path)) {
                        reload(site, path);
                    }
                }

                @Override
                public void deleted(Path path, boolean regularFile) {
                    if (regularFile) {
                        deleteItemByFile(site, path);
                    }
                }
            });
        }

        deleteSitesNotIn(sites);
    }

    @Async
    public void start(PathWatcher watcher) {
        LOG.info("Starting watcher for directory {}", watcher.getDirectory());

        try {
            while (!watcher.isClosed()) {
                watcher.watch();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            watcher.close();

            LOG.info("Closed watcher for directory {}", watcher.getDirectory());
        }
    }
}

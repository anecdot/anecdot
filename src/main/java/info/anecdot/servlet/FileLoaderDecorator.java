package info.anecdot.servlet;

import com.mitchellbosecke.pebble.loader.FileLoader;
import com.mitchellbosecke.pebble.loader.Loader;
import info.anecdot.content.Site;
import info.anecdot.content.SiteService;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.Reader;
import java.nio.file.Path;

/**
 * @author Stephan Grundner
 */
public class FileLoaderDecorator implements Loader<String> {

    private final FileLoader loader;
    private final SiteService siteService;

    @Override
    public Reader getReader(String templateName) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();

        Site site = siteService.findSiteByHost(request.getServerName());
        Path directory = site.getTheme();

        try {
            String prefix = directory.toRealPath().toString();
            if (!prefix.endsWith("/")) {
                prefix += "/";
            }
            templateName = prefix + templateName;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return loader.getReader(templateName);
    }

    @Override
    public void setCharset(String charset) {
        loader.setCharset(charset);
    }

    @Override
    public void setPrefix(String prefix) {
        loader.setPrefix(prefix);
    }

    @Override
    public void setSuffix(String suffix) {
        loader.setSuffix(suffix);
    }

    @Override
    public String resolveRelativePath(String relativePath, String anchorPath) {
        return loader.resolveRelativePath(relativePath, anchorPath);
    }

    @Override
    public String createCacheKey(String templateName) {
        return loader.createCacheKey(templateName);
    }

    public FileLoaderDecorator(FileLoader loader, SiteService siteService) {
        this.loader = loader;
        this.siteService = siteService;
    }
}

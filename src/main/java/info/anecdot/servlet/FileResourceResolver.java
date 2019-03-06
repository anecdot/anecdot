package info.anecdot.servlet;

import info.anecdot.content.Site;
import info.anecdot.content.SiteService;
import info.anecdot.gm.ResizeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author Stephan Grundner
 */
@Component
public class FileResourceResolver implements ResourceResolver {

    private static final String THEME_URL_PATH_PREFIX = "theme/";

    @Autowired
    private SiteService siteService;

    @Autowired
    private ResizeService resizeService;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public Resource resolveResource(HttpServletRequest request, String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {
        Site site = siteService.findSiteByHost(request.getServerName());

        if (site != null) {
            Path directory;

            if (requestPath.startsWith(THEME_URL_PATH_PREFIX)) {
                directory = site.getTheme();
                requestPath = resolveUrlPath(requestPath, null, null);
            } else {
                directory = Paths.get("./tmp/", site.getHost());
            }

            String location = "file:" + directory.toString();
            if (!location.endsWith("/")) {
                location += "/";
            }

            location += requestPath;

            if (resizeService.isResizeRequest(request)) {
                try {

                    return resizeService.resolveImageResource(location, request);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            return applicationContext.getResource(location);
        }

        return chain.resolveResource(request, requestPath, locations);
    }

    @Override
    public String resolveUrlPath(String resourcePath, List<? extends Resource> locations, ResourceResolverChain chain) {
        if (resourcePath != null && resourcePath.startsWith(THEME_URL_PATH_PREFIX)) {
            return resourcePath.substring(THEME_URL_PATH_PREFIX.length(), resourcePath.length());
        }

        return resourcePath;
    }
}

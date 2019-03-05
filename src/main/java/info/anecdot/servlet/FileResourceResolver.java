package info.anecdot.servlet;

import info.anecdot.content.Site;
import info.anecdot.content.SiteService;
import info.anecdot.image.ImagingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.resource.AbstractResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Stephan Grundner
 */
@Component
public class FileResourceResolver extends AbstractResourceResolver {

    private static final String THEME_URL_PATH_PREFIX = "theme/";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SiteService siteService;

    @Autowired
    private ImagingService imagingService;

    @Override
    protected Resource resolveResourceInternal(HttpServletRequest request, String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {
        Site site = siteService.findSiteByRequest(request);

        if (site != null && !site.isBusy()) {
            Path directory = requestPath.startsWith(THEME_URL_PATH_PREFIX)
                    ? site.getThemeDirectory()
                    : site.getContentDirectory();

            requestPath = resolveUrlPathInternal(requestPath, locations, chain);

            String location = "file:" + directory.toString();
            if (!location.endsWith("/")) {
                location += "/";
            }

            location += requestPath;

            if (imagingService.isImageRequest(request)) {

                try {
                    return imagingService.resolveImageResource(location, request);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            return applicationContext.getResource(location);
        }

        return chain.resolveResource(request, requestPath, locations);
    }

    @Override
    protected String resolveUrlPathInternal(String resourceUrlPath, List<? extends Resource> locations, ResourceResolverChain chain) {
        if (resourceUrlPath.startsWith(THEME_URL_PATH_PREFIX)) {
            return resourceUrlPath.substring(THEME_URL_PATH_PREFIX.length(), resourceUrlPath.length());
        }

        return resourceUrlPath;
    }
}

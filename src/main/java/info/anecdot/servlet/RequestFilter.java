package info.anecdot.servlet;

import info.anecdot.content.Item;
import info.anecdot.content.ItemService;
import info.anecdot.content.Site;
import info.anecdot.content.SiteService;
import info.anecdot.tracking.TrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Stephan Grundner
 */
@Component
//public class RequestFilter extends OncePerRequestFilter {
public class RequestFilter implements Filter {

    @Autowired
    private ServerProperties serverProperties;

    @Autowired
    private TrackingService trackingService;

    @Autowired
    private SiteService siteService;

    @Autowired
    private ViewResolver viewResolver;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemHandler itemHandler;

    private UrlPathHelper pathHelper = new UrlPathHelper();

    private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {

        ErrorProperties error = serverProperties.getError();
        String uri = pathHelper.getRequestUri(request);

        if (!uri.equals(error.getPath()) && !uri.startsWith("/theme")) {
            Site site = siteService.findSiteByRequest(request);
            if (site != null) {
                request.setAttribute(Site.class.getName(), site);

                trackingService.track(request, response);

                if (site.isBusy()) {
//                    TODO Render busy view
                    return;
                }

                Item item = itemService.findItemBySiteAndURI(site, uri);
                if (item != null) {

                    itemHandler.handleRequest(request, response);

                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }
}

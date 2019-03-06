package info.anecdot.sardine;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;

/**
 * @author Stephan Grundner
 */
public class DavResourceUtils {

    public static void walk(Sardine sardine, String url, DavResourceVisitor visitor) throws IOException {
        url = url.replaceAll(" ", "%20");

        if (!url.endsWith("/")) {
            url += "/";
        }

        List<DavResource> resources = sardine.list(url);

        DavResource root = resources.remove(0);
        String path = root.getPath();
        String baseUrl = StringUtils.removeEnd(url, path);

        for (DavResource resource : resources) {
            if (resource.isDirectory()) {
                walk(sardine, baseUrl + resource.getPath(), visitor);
            }

            visitor.visit(sardine, baseUrl, resource);
        }
    }
}

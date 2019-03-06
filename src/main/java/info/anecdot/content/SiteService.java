package info.anecdot.content;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import info.anecdot.sardine.DavResourceUtils;
import info.anecdot.sardine.DavResourceVisitor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Stephan Grundner
 */
@Service
public class SiteService {

    private static final Logger LOG = LoggerFactory.getLogger(SiteService.class);

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private AssetService assetService;

    @Autowired
    private ItemService itemService;

    public Site findSiteByHost(String host) {
        return siteRepository.findByHost(host);
    }

    public Site saveSite(Site site) {
        return siteRepository.save(site);
    }

    private void deleteSite(Site site) {
//        TODO Delete all items and assets too
        siteRepository.delete(site);
    }

    public void deleteSitesNotIn(List<Site> sites) {
        siteRepository.findAllNotIn(sites)
                .forEach(this::deleteSite);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sync(Site site) throws IOException {
        Webdav webdav = site.getWebdav();
        Sardine sardine = SardineFactory.begin(webdav.getUsername(), webdav.getPassword());

        Date now = Date.from(Instant.now());
        List<Asset> changed = new ArrayList<>();

        String webdavUrl = webdav.getUrl();
        DavResourceUtils.walk(sardine, webdavUrl, new DavResourceVisitor() {
            @Override
            public void visit(Sardine sardine, String baseUrl, DavResource resource) {
                if (resource.isDirectory()) {
                    return;
                }

                String absoluteUrl = baseUrl + resource.getPath();
                String relativePath = StringUtils.removeStart(absoluteUrl, webdavUrl);
                Asset asset = assetService.findAssetBySiteAndPath(site, relativePath);
                if (asset == null) {
                    asset = new Asset();
                    asset.setSite(site);
                    asset.setPath(relativePath);
                }

                Path path = Paths.get("./tmp", site.getHost(), asset.getPath());

                if (!StringUtils.equals(resource.getEtag(), asset.getEtag())) {
                    LOG.debug("Updating " + path);
                    if (!Files.exists(path)) {
                        try {
                            Files.createDirectories(path.getParent());

                            String url = baseUrl + resource.getPath();
                            try (InputStream in = sardine.get(url);
                                 OutputStream out = Files.newOutputStream(path)) {

                                IOUtils.copyLarge(in, out);
                            }

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    asset.setContentLength(resource.getContentLength());
                    asset.setModified(resource.getModified());
                    asset.setEtag(resource.getEtag());

                    changed.add(asset);
                } else {
                    LOG.debug("Not modified: " + path);
                }

                asset.setSynced(now);
                asset = assetService.saveAsset(asset);
            }
        });

        List<Asset> obsolete = assetService.findAllAssetsSyncedBefore(site, now);
        obsolete.forEach(itemService::deleteItemForAsset);
        assetService.deleteAssets(obsolete);

        changed.forEach(asset -> {
            String fileName = FilenameUtils.getBaseName(asset.getPath());
            String extension = FilenameUtils.getExtension(asset.getPath());

            if (!fileName.startsWith(".") && "xml".equalsIgnoreCase(extension)) {
                itemService.loadItem(asset);
            }
        });
    }
}

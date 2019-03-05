package info.anecdot.image;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;

/**
 * @author Stephan Grundner
 */
@Service
@Primary
public class GraphicsMagickService implements ImagingService {

    private class Conversion {

        private long lastModified;
        private File file;

        public long getLastModified() {
            return lastModified;
        }

        public void setLastModified(long lastModified) {
            this.lastModified = lastModified;
        }

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CacheManager cacheManager;

    @Override
    public boolean isImageRequest(HttpServletRequest request) {
        return request.getParameter("size") != null;
    }

    @Override
    public Resource resolveImageResource(String location, HttpServletRequest request) throws IOException {
        Cache cache = cacheManager.getCache("images");
        String url = request.getRequestURL()
                .append('?')
                .append(request.getQueryString())
                .toString();
        Conversion conversion = cache.get(url, Conversion.class);
        Resource original = applicationContext.getResource(location);
        if (conversion == null || original.lastModified() > conversion.lastModified) {
            String nameWithoutExtension = FilenameUtils.getBaseName(location);
            String extension = FilenameUtils.getExtension(location);
            String size = request.getParameter("size");
            File temp = File.createTempFile(nameWithoutExtension + size, "." + extension);
            temp.deleteOnExit();

            try {
                Process process = new ProcessBuilder()
                        .command(Arrays.asList(
                                "gm", "convert",
                                "-size", size,
                                original.getFile().getAbsolutePath(),
                                "-resize", size,
                                temp.getAbsolutePath()))
                        .redirectErrorStream(true)
                        .start();

                process.waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            conversion = new Conversion();
            conversion.setFile(temp);
            conversion.setLastModified(original.lastModified());
            cache.put(url, conversion);
        }

        return new FileUrlResource(conversion.file.getAbsolutePath());
    }
}

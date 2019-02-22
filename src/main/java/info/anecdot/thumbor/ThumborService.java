package info.anecdot.thumbor;

import info.anecdot.image.ImagingService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Paths;

/**
 * @author Stephan Grundner
 */
@Service
public class ThumborService implements ImagingService, ApplicationRunner {

    @Autowired
    private Environment environment;

    @Autowired
    private TaskExecutor taskExecutor;

    @Override
    public boolean isImageRequest(HttpServletRequest request) {
        return request.getParameter("size") != null;
    }

    @Override
    public Resource resolveImageResource(String location, HttpServletRequest request) {

        Integer port = environment.getProperty("thumbor.port", Integer.class);

        location = StringUtils.removeStart(location, "file:");
        location = StringUtils.removeStart(location, "./");

        String size = request.getParameter("size");
        String url = String.format("http://localhost:%d/unsafe/%s/%s", port, size, location);

        try {
            return new UrlResource(url) {
                @Override
                public long lastModified() throws IOException {
                    return -1;
                }

                @Override
                public synchronized long contentLength() throws IOException {
                    return -1;
                }

                @Override
                public boolean isReadable() {
                    return true;
                }

                @Override
                public Resource createRelative(String relativePath) throws MalformedURLException {
                    throw new UnsupportedOperationException();
                }
            };
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private void execute() throws IOException, InterruptedException {
        File configFile = File.createTempFile("thumbor", ".conf");
        configFile.deleteOnExit();
        try (FileOutputStream outputStream = new FileOutputStream(configFile);
             PrintStream printer = new PrintStream(outputStream)) {

            printer.println("LOADER='thumbor.loaders.file_loader'");
            printer.println("FILE_LOADER_ROOT_PATH='/'");
            printer.printf("STORAGE='%s'\n", "thumbor.storages.file_storage");
//            printer.printf("STORAGE_EXPIRATION_SECONDS=%d\n", 60 * 60);
//            printer.printf("FILE_STORAGE_ROOT_PATH='%s'\n", "/tmp");
            printer.printf("RESULT_STORAGE='%s'\n", "thumbor.result_storages.file_storage");
//            printer.printf("RESULT_STORAGE_FILE_STORAGE_ROOT_PATH='%s'\n", "/tmp");
            printer.printf("RESULT_STORAGE_STORES_UNSAFE=%s\n", "True");;
        }

        Integer port = environment.getProperty("thumbor.port", Integer.class);
        String loggingLevel = environment.getProperty("thumbor.debug", Boolean.class, false) ? "DEBUG" : "INFO";
        ProcessBuilder builder = new ProcessBuilder()
                .command("thumbor",
                        "-p", Integer.toString(port),
                        "-l", loggingLevel,
                        "-c", configFile.toString());

        File directory = Paths.get(".").toRealPath().toFile();
        builder.directory(directory);
        builder.redirectErrorStream(true);

        Process process = builder.start();

        Logger LOG = LoggerFactory.getLogger("thumbor");
        try (InputStreamReader reader = new InputStreamReader(process.getInputStream());
             BufferedReader buffer = new BufferedReader(reader);
             PrintStream printer = new PrintStream(System.out)) {

            String line;
            while (process.isAlive() && (line = buffer.readLine()) != null) {
                printer.println(line);
                LOG.debug(line);
            }
        }

        int exitValue = process.waitFor();
        LOG.debug("Thumbor exited: " + exitValue);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        taskExecutor.execute(() -> {
            try {
                execute();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}

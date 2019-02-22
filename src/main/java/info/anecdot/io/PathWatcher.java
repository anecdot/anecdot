package info.anecdot.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Stephan Grundner
 */
public class PathWatcher implements Closeable, AutoCloseable {

    public interface WatchHandler {

        void visited(Path path);
        void created(Path path);
        void modified(Path path);
        void deleted(Path path, boolean regularFile);
        void overflow();
    }

    public static class AbstractWatchHandler implements WatchHandler {

        @Override
        public void visited(Path path) { }

        @Override
        public void created(Path path) { }

        @Override
        public void modified(Path path) { }

        @Override
        public void deleted(Path path, boolean regularFile) { }

        @Override
        public void overflow() { }
    }

    private static final Logger LOG = LoggerFactory.getLogger(PathWatcher.class);

    private final WatchService watchService;
    private volatile boolean closed;

    private Path directory;

    private final Set<WatchKey> keys = new LinkedHashSet<>();

    private WatchHandler handler;

    public boolean isClosed() {
        return closed;
    }

    public Path getDirectory() {
        return directory;
    }

    public WatchHandler getHandler() {
        return handler;
    }

    public void setHandler(WatchHandler handler) {
        this.handler = handler;
    }

    private void register(Path directory) {
        WatchKey key;

        try {
            LOG.info("Begin observing {}", directory);

            Optional.ofNullable(handler)
                    .ifPresent(handler -> handler.visited(directory));

            Files.list(directory)
                    .filter(Files::isRegularFile)
                    .forEach(path -> Optional.ofNullable(handler)
                            .ifPresent(handler -> handler.visited(path)));

            Files.list(directory)
                    .filter(Files::isDirectory)
                    .forEach(this::register);

            key = directory.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.OVERFLOW);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        keys.add(key);
    }

    private boolean removeKeys(Path directory) {
        return keys.removeIf(it -> {
            if (((Path) it.watchable()).startsWith(directory)) {
                it.cancel();

                LOG.info("Stopped observing {}", it.watchable());

                return true;
            }

            return false;
        });
    }

    private void process(WatchKey key) {
        if (key != null) {
            try {
                for (WatchEvent<?> event : key.pollEvents()) {
                    Object context = event.context();
                    if (context instanceof Path) {
                        WatchEvent.Kind<?> kind = event.kind();

                        Path parent = (Path) key.watchable();
                        Path path = parent.resolve((Path) context);

                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            if (Files.isDirectory(path)) {
                                register(path);
                            }
                            Optional.ofNullable(handler)
                                    .ifPresent(it -> it.created(path));
                        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            Optional.ofNullable(handler)
                                    .ifPresent(it -> it.modified(path));
                        } else /* if (kind == StandardWatchEventKinds.ENTRY_DELETE) */ {
                            Path watchable = keys.stream()
                                    .filter(it -> it.watchable().equals(path))
                                    .map(WatchKey::watchable)
                                    .map(Path.class::cast)
                                    .findFirst().orElse(null);

                            if (watchable != null) {
                                boolean removed = removeKeys(watchable);

                                if (removed) {
                                    Optional.ofNullable(handler)
                                            .ifPresent(it -> it.deleted(path, false));
                                }

                            } else {
                                Optional.ofNullable(handler)
                                        .ifPresent(it -> it.deleted(path, true));
                            }
                        }
                    } else {
                        removeKeys(directory);
                        register(directory);
                        Optional.ofNullable(handler)
                                .ifPresent(WatchHandler::overflow);
                    }
                }
            } catch (ClosedWatchServiceException e) {
                close();
            } finally {
                key.reset();
            }
        }
    }

    public void watch(Duration wait) throws InterruptedException {
        if (keys.isEmpty()) {
            register(directory);
        }

        if (wait == null) {
            process(watchService.take());
        } else if (wait.isZero() || wait.isNegative()) {
            process(watchService.poll());
        } else {
            process(watchService.poll(wait.toNanos(), TimeUnit.NANOSECONDS));
        }
    }

    public void watch() throws InterruptedException {
        watch(null);
    }

    @Override
    public void close() {
        try {
            watchService.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            closed = true;
        }
    }

    public PathWatcher(Path directory) throws IOException {
        this.directory = directory;

        FileSystem fileSystem = directory.getFileSystem();
        watchService = fileSystem.newWatchService();
    }
}

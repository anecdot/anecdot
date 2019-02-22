package info.anecdot.image;

import org.springframework.core.io.Resource;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Stephan Grundner
 */
public interface ImagingService {

    boolean isImageRequest(HttpServletRequest request);

    Resource resolveImageResource(String location, HttpServletRequest request);
}

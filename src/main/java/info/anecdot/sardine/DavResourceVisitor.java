package info.anecdot.sardine;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;

/**
 * @author Stephan Grundner
 */
public interface DavResourceVisitor {

    void visit(Sardine sardine, String baseUrl, DavResource resource);
}

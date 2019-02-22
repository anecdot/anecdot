package info.anecdot.tracking;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Stephan Grundner
 */
public interface VisitorRepository extends JpaRepository<Visitor, Long> {

    Visitor findByCookieValue(String cookieValue);
}

package info.anecdot.tracking;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Stephan Grundner
 */
public interface TrackRepository extends JpaRepository<Track, Long> {

}

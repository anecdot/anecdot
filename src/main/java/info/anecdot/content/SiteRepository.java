package info.anecdot.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * @author Stephan Grundner
 */
@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {

    Site findByName(String name);

    @Query("from Site s where s not in ?1")
    List<Site> findAllNotIn(Collection<Site> sites);
}

package info.anecdot.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.List;

/**
 * @author Stephan Grundner
 */
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    @Query("from Item where uri = ?1")
    Item findItemByURI(URI uri);

    @Query("from Item i where i.uri like ?1")
    List<Item> findAllItemsByURILike(String uri);

    @Query("select i from Item i " +
            "join i.site s " +
            "where i.site = ?1 " +
            "and i.syncId != s.syncId")
    List<Item> findAllObsolete(Site site);
}

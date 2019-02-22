package info.anecdot.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Stephan Grundner
 */
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    @Query("from Item where uri = ?1")
    Item findItemByURI(String uri);

    @Query("from Item i where i.uri like ?1")
    List<Item> findAllItemsByURILike(String uri);
}

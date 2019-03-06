package info.anecdot.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Stephan Grundner
 */
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findAllByAsset_Site(Site site);
    Item findByAsset(Asset asset);
}

package info.anecdot.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * @author Stephan Grundner
 */
@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    Asset findBySiteAndPath(Site site, String path);

    List<Asset> findAllBySiteAndSyncedBefore(Site site, Date synced);
}

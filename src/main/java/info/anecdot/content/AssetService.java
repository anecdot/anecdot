package info.anecdot.content;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @author Stephan Grundner
 */
@Service
public class AssetService {

    @Autowired
    private AssetRepository assetRepository;

    public Asset findAssetBySiteAndPath(Site site, String path) {
        return assetRepository.findBySiteAndPath(site, path);
    }

    public List<Asset> findAllAssetsSyncedBefore(Site site, Date synced) {
        return assetRepository.findAllBySiteAndSyncedBefore(site, synced);
    }

    public Asset saveAsset(Asset asset) {
        return assetRepository.save(asset);
    }

    public void deleteAssets(List<Asset> assets) {
        assetRepository.deleteAll(assets);
    }
}

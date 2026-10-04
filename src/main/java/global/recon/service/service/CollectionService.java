package global.recon.service.service;

import global.recon.service.model.CollectionCycle;
import global.recon.service.model.CollectionCycleDetail;
import global.recon.service.model.CollectionDetail;
import global.recon.service.model.CollectionPairRequest;
import global.recon.service.model.CreateCollectionCycleRequest;
import global.recon.service.model.CreateCollectionRequest;
import global.recon.service.model.ReconCollection;
import global.recon.service.model.ReconJob;
import global.recon.service.model.UpdateCollectionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CollectionService {

    ReconCollection create(CreateCollectionRequest request);

    List<ReconCollection> list();

    CollectionDetail get(String collectionId);

    ReconCollection update(String collectionId, UpdateCollectionRequest request);

    ReconCollection replacePairs(String collectionId, List<CollectionPairRequest> pairs);

    ReconJob startCycle(String collectionId, CreateCollectionCycleRequest request);

    Page<CollectionCycle> listCycles(String collectionId, Pageable pageable);

    CollectionCycleDetail getCycle(String collectionId, String cycleId);
}

package global.recon.service.config;

import global.recon.service.model.CollectionMember;
import global.recon.service.model.ReconCollection;
import global.recon.service.repository.CollectionMemberRepository;
import global.recon.service.service.ResourceNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class CollectionAccess {

    private final CollectionMemberRepository collectionMemberRepository;

    public CollectionAccess(CollectionMemberRepository collectionMemberRepository) {
        this.collectionMemberRepository = collectionMemberRepository;
    }

    public void assertCanView(ReconCollection collection) {
        String email = UserContext.require();
        if (email.equals(collection.getOwnerEmail())) {
            return;
        }
        collectionMemberRepository.findByCollectionIdAndEmail(collection.getId(), email)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
    }

    public void assertCanEdit(ReconCollection collection) {
        String email = UserContext.require();
        if (!email.equals(collection.getOwnerEmail())) {
            throw new ResourceNotFoundException("Resource not found");
        }
    }

    public boolean isMember(String collectionId, String email) {
        if (email == null) {
            return false;
        }
        return collectionMemberRepository.findByCollectionIdAndEmail(collectionId, email).isPresent();
    }
}

package global.recon.service.model;

import java.util.ArrayList;
import java.util.List;

public class CollectionDetail {

    private ReconCollection collection;
    private List<CollectionMember> members = new ArrayList<>();
    private List<CollectionPair> pairs = new ArrayList<>();
    private CollectionCycle lastCycle;

    public ReconCollection getCollection() {
        return collection;
    }

    public void setCollection(ReconCollection collection) {
        this.collection = collection;
    }

    public List<CollectionMember> getMembers() {
        return members;
    }

    public void setMembers(List<CollectionMember> members) {
        this.members = members;
    }

    public List<CollectionPair> getPairs() {
        return pairs;
    }

    public void setPairs(List<CollectionPair> pairs) {
        this.pairs = pairs;
    }

    public CollectionCycle getLastCycle() {
        return lastCycle;
    }

    public void setLastCycle(CollectionCycle lastCycle) {
        this.lastCycle = lastCycle;
    }
}

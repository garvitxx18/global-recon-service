package global.recon.service.model;

import java.util.ArrayList;
import java.util.List;

public class CollectionCycleDetail {

    private CollectionCycle cycle;
    private List<CollectionItem> items = new ArrayList<>();

    public CollectionCycle getCycle() {
        return cycle;
    }

    public void setCycle(CollectionCycle cycle) {
        this.cycle = cycle;
    }

    public List<CollectionItem> getItems() {
        return items;
    }

    public void setItems(List<CollectionItem> items) {
        this.items = items;
    }
}

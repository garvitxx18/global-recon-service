package global.recon.service.model;

import java.time.LocalDate;

public class CreateCollectionCycleRequest {

    private LocalDate asOfDate;

    public LocalDate getAsOfDate() {
        return asOfDate;
    }

    public void setAsOfDate(LocalDate asOfDate) {
        this.asOfDate = asOfDate;
    }
}

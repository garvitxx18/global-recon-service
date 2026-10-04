package global.recon.service.model;

import java.util.ArrayList;
import java.util.List;

public class UpdateCollectionRequest {

    private String name;
    private CollectionDatePolicy datePolicy;
    private String scheduleCron;
    private List<String> memberEmails = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CollectionDatePolicy getDatePolicy() {
        return datePolicy;
    }

    public void setDatePolicy(CollectionDatePolicy datePolicy) {
        this.datePolicy = datePolicy;
    }

    public String getScheduleCron() {
        return scheduleCron;
    }

    public void setScheduleCron(String scheduleCron) {
        this.scheduleCron = scheduleCron;
    }

    public List<String> getMemberEmails() {
        return memberEmails;
    }

    public void setMemberEmails(List<String> memberEmails) {
        this.memberEmails = memberEmails == null ? new ArrayList<>() : memberEmails;
    }
}

package global.recon.service.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CreateCollectionRequest {

    private String name;
    private String planId;
    private String leftIdentityParam;
    private String rightIdentityParam;
    private String leftDateParam;
    private String rightDateParam;
    private CollectionDatePolicy datePolicy = CollectionDatePolicy.T1;
    private String scheduleCron;
    private Map<String, String> constantParams = new LinkedHashMap<>();
    private List<String> memberEmails = new ArrayList<>();
    private List<CollectionPairRequest> pairs = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getLeftIdentityParam() {
        return leftIdentityParam;
    }

    public void setLeftIdentityParam(String leftIdentityParam) {
        this.leftIdentityParam = leftIdentityParam;
    }

    public String getRightIdentityParam() {
        return rightIdentityParam;
    }

    public void setRightIdentityParam(String rightIdentityParam) {
        this.rightIdentityParam = rightIdentityParam;
    }

    public String getLeftDateParam() {
        return leftDateParam;
    }

    public void setLeftDateParam(String leftDateParam) {
        this.leftDateParam = leftDateParam;
    }

    public String getRightDateParam() {
        return rightDateParam;
    }

    public void setRightDateParam(String rightDateParam) {
        this.rightDateParam = rightDateParam;
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

    public Map<String, String> getConstantParams() {
        return constantParams;
    }

    public void setConstantParams(Map<String, String> constantParams) {
        this.constantParams = constantParams == null ? new LinkedHashMap<>() : constantParams;
    }

    public List<String> getMemberEmails() {
        return memberEmails;
    }

    public void setMemberEmails(List<String> memberEmails) {
        this.memberEmails = memberEmails == null ? new ArrayList<>() : memberEmails;
    }

    public List<CollectionPairRequest> getPairs() {
        return pairs;
    }

    public void setPairs(List<CollectionPairRequest> pairs) {
        this.pairs = pairs == null ? new ArrayList<>() : pairs;
    }
}

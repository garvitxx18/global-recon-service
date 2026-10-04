package global.recon.service.model;

import java.util.ArrayList;
import java.util.List;

public class SourceView {

    private String id;
    private String name;
    private String vendor;
    private String path;
    private String httpMethod;
    private String recordPath;
    private SourceAuthType authType;
    private List<SourceParam> params = new ArrayList<>();

    public static SourceView from(Source source, List<SourceParam> params) {
        SourceView view = new SourceView();
        view.setId(source.getId());
        view.setName(source.getName());
        view.setVendor(source.getVendor());
        view.setPath(source.getPath());
        view.setHttpMethod(source.getHttpMethod());
        view.setRecordPath(source.getRecordPath());
        view.setAuthType(source.getAuthType());
        view.setParams(params == null ? List.of() : params);
        return view;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getRecordPath() {
        return recordPath;
    }

    public void setRecordPath(String recordPath) {
        this.recordPath = recordPath;
    }

    public SourceAuthType getAuthType() {
        return authType;
    }

    public void setAuthType(SourceAuthType authType) {
        this.authType = authType;
    }

    public List<SourceParam> getParams() {
        return params;
    }

    public void setParams(List<SourceParam> params) {
        this.params = params == null ? new ArrayList<>() : params;
    }
}

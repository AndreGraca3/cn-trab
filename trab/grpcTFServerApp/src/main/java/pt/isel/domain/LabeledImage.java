package pt.isel.domain;

import java.util.Date;
import java.util.List;

public class LabeledImage {

    private String requestId;
    private List<Label> labels;
    private Date processedAt;

    public LabeledImage() {
    }

    public String getRequestId() {
        return requestId;
    }

    public List<Label> getLabels() {
        return labels;
    }

    public Date getProcessedAt() {
        return processedAt;
    }
}

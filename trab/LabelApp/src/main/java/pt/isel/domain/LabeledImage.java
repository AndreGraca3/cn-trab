package pt.isel.domain;

import java.util.Date;
import java.util.List;

public class LabeledImage {

    private final String requestId;
    private final List<Label> labels;
    private final Date processedAt;

    public LabeledImage(String requestId, List<Label> labels) {
        this.requestId = requestId;
        this.labels = labels;
        this.processedAt = new Date();
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

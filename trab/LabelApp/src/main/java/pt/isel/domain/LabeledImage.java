package pt.isel.domain;

import java.util.Date;
import java.util.List;

public class LabeledImage {

    private final String requestId;
    private final List<Label> labels;
    private final String fileName;
    private final Date processedAt;

    public LabeledImage(String requestId, String fileName, List<Label> labels) {
        this.requestId = requestId;
        this.labels = labels;
        this.fileName = fileName;
        this.processedAt = new Date();
    }

    public String getRequestId() {
        return requestId;
    }

    public List<Label> getLabels() {
        return labels;
    }

    public String getFileName() {
        return fileName;
    }

    public Date getProcessedAt() {
        return processedAt;
    }
}

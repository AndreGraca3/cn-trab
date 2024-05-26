package pt.isel.domain;

import java.util.Date;
import java.util.List;

public class LabeledImage {

    private String requestId;
    private List<Label> labels;
    private String fileName;
    private Date processedAt;

    public LabeledImage() {
    }

    public LabeledImage(String requestId, List<Label> newLabels, String fileName, Date processedAt) {
        this.requestId = requestId;
        this.labels = newLabels;
        this.fileName = fileName;
        this.processedAt = processedAt;
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

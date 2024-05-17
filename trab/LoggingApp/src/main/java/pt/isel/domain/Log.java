package pt.isel.domain;

public class Log {

    private String requestId;
    private String bucketName;
    private String blobName;

    public Log(String requestId, String bucketName, String blobName) {
        this.requestId = requestId;
        this.bucketName = bucketName;
        this.blobName = blobName;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getBlobName() {
        return blobName;
    }
}

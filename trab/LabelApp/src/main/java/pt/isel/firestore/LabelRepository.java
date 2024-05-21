package pt.isel.firestore;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import pt.isel.domain.Label;
import pt.isel.domain.LabeledImage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class LabelRepository {

    public Firestore db;

    public LabelRepository(Firestore db) {
        this.db = db;
    }

    public void saveLabeledImage(LabeledImage labeledImage, String collectionName) throws ExecutionException, InterruptedException {
        Map<String, Object> labeledImageMap = new HashMap<>();
        labeledImageMap.put("requestId", labeledImage.getRequestId());
        labeledImageMap.put("fileName", labeledImage.getFileName());
        labeledImageMap.put("processedAt", labeledImage.getProcessedAt());

        DocumentReference labeledImageDocRef = db.collection(collectionName).document(labeledImage.getRequestId());
        labeledImageDocRef.set(labeledImageMap);

        // Add labels as subCollection so we can query them later
        CollectionReference labelsCollection = labeledImageDocRef.collection("labels");
        for (Label label : labeledImage.getLabels()) {
            Map<String, Object> labelMap = new HashMap<>();
            labelMap.put("value", label.getValue());
            labelMap.put("translation", label.getTranslation());
            labelsCollection.add(labelMap);
        }
    }
}

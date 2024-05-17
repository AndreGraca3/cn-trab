package pt.isel.firestore;

import com.google.cloud.firestore.Firestore;
import pt.isel.domain.LabeledImage;

import java.util.concurrent.ExecutionException;

public class LabelRepository {
    private final Firestore db;

    public LabelRepository(Firestore db) {
        this.db = db;
    }

    public LabeledImage getLabeledImage(String requestId, String collectionName) throws ExecutionException, InterruptedException {
        var doc = db.collection(collectionName).document(requestId).get().get();
        if (doc.exists()) {
            return doc.toObject(LabeledImage.class);
        }
        return null;
    }
}

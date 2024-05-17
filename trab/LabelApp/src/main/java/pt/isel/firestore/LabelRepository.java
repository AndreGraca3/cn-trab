package pt.isel.firestore;

import com.google.cloud.firestore.Firestore;
import pt.isel.domain.LabeledImage;

import java.util.concurrent.ExecutionException;

public class LabelRepository {

    public Firestore db;

    public LabelRepository(Firestore db) {
        this.db = db;
    }

    public void saveLabeledImage(LabeledImage labeledImage, String collectionName) throws ExecutionException, InterruptedException {
        db.collection(collectionName).document(labeledImage.getRequestId()).set(labeledImage).get();
    }
}

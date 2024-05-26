package pt.isel.firestore;


import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import pt.isel.domain.Label;
import pt.isel.domain.LabeledImage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class LabelRepository {
    private final Firestore db;

    public LabelRepository(Firestore db) {
        this.db = db;
    }

    public LabeledImage getLabeledImage(String requestId, String collectionName) throws ExecutionException, InterruptedException {
        var doc = db.collection(collectionName).document(requestId).get().get();

        var image = doc.toObject(LabeledImage.class);
        if (image == null) return null;

        var labels = doc.getReference()
                .collection("labels")
                .get().get().getDocuments()
                .stream().map(it -> it.toObject(Label.class)).collect(Collectors.toList());

        return new LabeledImage(
                image.getRequestId(),
                labels,
                image.getFileName(),
                image.getProcessedAt()
        );
    }

    public List<LabeledImage> getLabeledImagesByLabel(
            String label,
            Date startDate,
            Date endDate,
            String collectionName
    ) throws ExecutionException, InterruptedException {
        List<LabeledImage> labeledImages = new ArrayList<>();

        QuerySnapshot querySnapshot = db.collection(collectionName)
                .whereGreaterThanOrEqualTo("processedAt", startDate)
                .whereLessThanOrEqualTo("processedAt", endDate)
                .get()
                .get();

        // Get the labels subCollection for each document and check if the label exists
        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
            QuerySnapshot labelSnapshot = document.getReference().collection("labels")
                    .whereEqualTo("value", label)
                    .get()
                    .get();

            if (!labelSnapshot.isEmpty()) {
                LabeledImage labeledImage = document.toObject(LabeledImage.class);
                labeledImages.add(labeledImage);
            }
        }

        return labeledImages;
    }
}

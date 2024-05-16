package pt.isel;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutionException;

public class FirebaseOperations {
    private final Firestore db;
    private final String collectionName;

    public FirebaseOperations(Firestore db, String collectionName) {
        this.db = db;
        this.collectionName = collectionName;
    }

    public void insertDocuments(String pathnameCSV)
            throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(pathnameCSV));
        CollectionReference colRef = db.collection(collectionName);
        String line;
        while ((line = reader.readLine()) != null) {
            // read document...
        }
    }

    public void printDocument(String id) throws ExecutionException, InterruptedException {
        DocumentReference documentReference = db.collection(collectionName).document(id);
        ApiFuture<DocumentSnapshot> future = documentReference.get();
        DocumentSnapshot document = future.get();
        // return object here...
    }

    public void getDocumentByField(String fieldValue, String... fieldNames) throws ExecutionException, InterruptedException {
        String field = fieldNames[fieldNames.length - 1];
        FieldPath fp = FieldPath.of(
                Arrays.toString(
                                Arrays.copyOf(fieldNames, fieldNames.length - 1))
                        .replace("[", "")
                        .replace("]", ""),
                field
        );
        Query query = db.collection(collectionName).whereEqualTo(fp, fieldValue);
        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            // return documents here...
        }
    }

    public void getDocumentByMultipleFields(Integer idValue, String freguesiaValue, String tipoEventoValue) throws ExecutionException, InterruptedException {
        FieldPath fpfreg = FieldPath.of("location", "freguesia");
        FieldPath fptipo = FieldPath.of("event", "tipo");

        Query query = db.collection(collectionName)
                .whereGreaterThan("ID", idValue)
                .whereEqualTo(fpfreg, freguesiaValue)
                .whereEqualTo(fptipo, tipoEventoValue);

        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            // return documents here...
        }
    }

    public void eventsThatStartedInDate(Date startDate, Date endDate) throws ExecutionException, InterruptedException {
        Query query = db.collection(collectionName)
                .whereGreaterThan("event.dtInicio", startDate)
                .whereLessThan("event.dtInicio", endDate);

        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            // return document here...
        }
    }

    public void eventsHappeningDuringDate(Date startDate, Date endDate) throws ExecutionException, InterruptedException {
        Query query = db.collection(collectionName)
                .whereGreaterThan("event.dtInicio", startDate)
                .whereLessThan("event.dtFinal", endDate);

        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            // return document here...
        }
    }

}

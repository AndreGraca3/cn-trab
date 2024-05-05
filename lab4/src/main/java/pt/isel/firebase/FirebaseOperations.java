package pt.isel.firebase;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import pt.isel.domain.OcupacaoTemporaria;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static pt.isel.utils.Utils.convertLineToObject;

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
            OcupacaoTemporaria ocupacaoTemporaria = convertLineToObject(line);
            DocumentReference docRef = colRef.document("Lab4-" + ocupacaoTemporaria.ID);
            ApiFuture<WriteResult> resultFut = docRef.set(ocupacaoTemporaria);
            WriteResult result = resultFut.get();
            System.out.println("Update time : " + result.getUpdateTime());
        }
    }

    public void printDocument(String id) throws ExecutionException, InterruptedException {
        DocumentReference documentReference = db.collection(collectionName).document(id);
        ApiFuture<DocumentSnapshot> future = documentReference.get();
        DocumentSnapshot document = future.get();
        OcupacaoTemporaria ocupacaoTemporaria = document.toObject(OcupacaoTemporaria.class);
        System.out.println(ocupacaoTemporaria);
    }

    public void deleteDocumentField(String id, String fieldName) throws ExecutionException, InterruptedException {
        DocumentReference documentReference = db.collection(collectionName).document(id);
        Map<String, Object> updates = new HashMap<>();
        updates.put(fieldName, FieldValue.delete());
        ApiFuture<WriteResult> writeResult = documentReference.update(updates);
        System.out.println("Update time : " + writeResult.get());
    }

    public void printDocumentByField(String fieldValue, String... fieldNames) throws ExecutionException, InterruptedException {
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
            System.out.println(doc);
        }
    }

    public void printDocumentByMultipleFields(Integer idValue, String freguesiaValue, String tipoEventoValue) throws ExecutionException, InterruptedException {

        FieldPath fpfreg = FieldPath.of("location", "freguesia");
        FieldPath fptipo = FieldPath.of("event", "tipo");

        Query query = db.collection(collectionName)
                .whereGreaterThan("ID", idValue)
                .whereEqualTo(fpfreg, freguesiaValue)
                .whereEqualTo(fptipo, tipoEventoValue);

        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            System.out.println(doc.getData());
        }
    }

    public void EventsthatstartedinDate(Date startDate, Date endDate) throws ExecutionException, InterruptedException {
        Query query = db.collection(collectionName)
                .whereGreaterThan("event.dtInicio", startDate)
                .whereLessThan("event.dtInicio", endDate);

        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            System.out.println(doc.getData());
        }
    }

    public void EventshappeningduringDate(Date startDate, Date endDate) throws ExecutionException, InterruptedException {
        Query query = db.collection(collectionName)
                .whereGreaterThan("event.dtInicio", startDate)
                .whereLessThan("event.dtFinal", endDate);

        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            System.out.println(doc.getData());
        }
    }

}

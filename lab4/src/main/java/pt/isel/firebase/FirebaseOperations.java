package pt.isel.firebase;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import pt.isel.domain.OcupacaoTemporaria;

import java.io.BufferedReader;
import java.io.FileReader;

import static pt.isel.utils.Utils.convertLineToObject;

public class FirebaseOperations {
    public static void insertDocuments(String pathnameCSV, Firestore db, String collectionName)
            throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(pathnameCSV));
        CollectionReference colRef = db.collection(collectionName);
        String line;
        while ((line = reader.readLine()) != null) {
            OcupacaoTemporaria ocup = convertLineToObject(line);
            DocumentReference docRef = colRef.document("Lab4-" + ocup.ID);
            ApiFuture<WriteResult> resultFut = docRef.set(ocup);
            WriteResult result = resultFut.get();
            System.out.println("Update time : " + result.getUpdateTime());
        }
    }
}

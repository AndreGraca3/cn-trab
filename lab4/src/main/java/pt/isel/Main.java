package pt.isel;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import pt.isel.firebase.FirebaseOperations;

public class Main {

    private static final String csvPath = "src/main/java/pt/isel/in/OcupacaoEspacosPublicos.csv";
    private static final String collectionName = "OcupacaoTemporaria";
    private static final String databaseId = "cn-geral-db";

    public static void main(String[] args) {
        try {

            GoogleCredentials credentials =
                    GoogleCredentials.getApplicationDefault();

            FirestoreOptions options = FirestoreOptions
                    .newBuilder().setDatabaseId(databaseId).setCredentials(credentials)
                    .build();
            Firestore db = options.getService();

            FirebaseOperations firebaseOperations = new FirebaseOperations(db, collectionName);

            firebaseOperations.printDocumentByField("Parque das Nações", "location", "freguesia");

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
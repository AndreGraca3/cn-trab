package pt.isel;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import pt.isel.firebase.FirebaseOperations;

import java.text.SimpleDateFormat;
import java.util.Date;

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

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            Date startDate = dateFormat.parse("31/01/2017");
            Date endDate = dateFormat.parse("01/03/2017");

            firebaseOperations.printDocumentByMultipleFields(2000, "Alvalade", "Publicitário");
            //firebaseOperations.EventshappeningduringDate(startDate,endDate);
            //firebaseOperations.EventsthatstartedinDate(startDate,endDate);



            //firebaseOperations.printDocumentByField("Parque das Nações", "location", "freguesia");

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
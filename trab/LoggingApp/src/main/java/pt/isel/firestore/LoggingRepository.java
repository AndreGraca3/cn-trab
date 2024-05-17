package pt.isel.firestore;

import com.google.cloud.firestore.Firestore;
import pt.isel.domain.Log;

import java.util.concurrent.ExecutionException;

public class LoggingRepository {

    public Firestore db;

    public LoggingRepository(Firestore db) {
        this.db = db;
    }

    public void saveLog(Log log, String collectionName) throws ExecutionException, InterruptedException {
        db.collection(collectionName).document(log.getRequestId()).set(log).get();
    }
}

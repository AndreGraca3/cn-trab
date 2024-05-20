package pt.isel.firestore;


import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import pt.isel.domain.LabeledImage;
import java.util.*;


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



    public ArrayList<String> getImageId(String valuelabel,String startdate,String enddate, String collectionName) throws ExecutionException, InterruptedException {


        ArrayList<String> res = new ArrayList<>();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date startDate;
        try {
            startDate = dateFormat.parse(startdate);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        Date endDate;
        try {
            endDate = dateFormat.parse(enddate);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        Timestamp startTimestamp = Timestamp.of(startDate);
        Timestamp endTimestamp = Timestamp.of(endDate);

        QuerySnapshot querySnapshot = db.collection(collectionName).get().get();
        List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();

        for (QueryDocumentSnapshot document : documents) {
            Timestamp processedAt = document.getTimestamp("processedAt");
            if (processedAt != null && processedAt.compareTo(startTimestamp) >= 0 && processedAt.compareTo(endTimestamp) <= 0) {
                List<Map<String, Object>> labels = (List<Map<String, Object>>) document.get("labels");
                if (labels != null) {
                    for (Map<String, Object> label : labels) {
                        if (valuelabel.equals(label.get("value"))) {
                            res.add(document.getId());
                            break;
                        }
                    }
                }
            }
        }


        return res;
    }


    /*
    //TENTATIVA DE FAZER A FILTRAGEM NO FIRESTORE - TENTATIVA FALHADA :(
    public ArrayList<String> getImageId(String label,String startdate,String enddate, String collectionName) throws ExecutionException, InterruptedException {


        //FieldPath fplabel = FieldPath.of("processedAt");

        ArrayList<String> res = new ArrayList<>();

        String dateString = "2022-01-01 00:00:00";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date parsedDate = dateFormat.parse(dateString);
        Timestamp timestamp = new Timestamp(parsedDate);


        Query query = db.collection(collectionName)
                //.whereNotEqualTo("requestId", "cn-2024-bucket-g15-eu8d9e2855-81dc-46c9-8dcd-c642910bad84");
                //.whereEqualTo("labels.0.value", "Lip");
                //.whereArrayContains("labels", new HashMap<String, String>() {{
                //    put("value", "Lip");
                //}});
                .whereArrayContains("labels.value", "Lip");
                //.whereEqualTo("labels.value", label);
                //.whereGreaterThan("processedAt", startdate)
                //.whereLessThan("processedAt", enddate);

        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            System.out.println(doc.getId());
            res.add(doc.getId());
            //add to list
        }
        return res;
    }*/
}

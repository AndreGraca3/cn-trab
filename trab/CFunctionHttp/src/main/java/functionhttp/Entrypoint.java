package functionhttp;

import com.google.cloud.compute.v1.*;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;


import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Map;

public class Entrypoint implements HttpFunction {

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        BufferedWriter writer = response.getWriter();

        String gcpProjectID =
                request.getFirstQueryParameter("projectID").orElse("cn2324-t1-g15");

        String instanceGroup =
                request.getFirstQueryParameter("instanceGroup").orElse("instance-group-server");

        ArrayList<String> ipAddresses = new ArrayList<>();
        try (InstancesClient client = InstancesClient.create()) {
            InstancesClient.AggregatedListPagedResponse listPagedResponse = client.aggregatedList(gcpProjectID);

            for (Map.Entry<String, InstancesScopedList> zoneInstances : listPagedResponse.iterateAll()) {
                for (Instance instance : zoneInstances.getValue().getInstancesList()) {
                    if (instance.getName().contains(instanceGroup) &&
                            instance.getStatus().compareTo("RUNNING") == 0) {
                        String ip = instance.getNetworkInterfaces(0).getAccessConfigs(0).getNatIP();
                        ipAddresses.add(ip);
                    }
                }
            }
        }
        writer.write(new Gson().toJson(new InstancesCollection(ipAddresses)));
    }
}

class InstancesCollection {
    public ArrayList<String> instanceIps;

    public InstancesCollection(ArrayList<String> ipAddresses) {
        this.instanceIps = ipAddresses;
    }
}

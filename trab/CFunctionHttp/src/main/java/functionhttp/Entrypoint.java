package functionhttp;

import com.google.cloud.compute.v1.*;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;


import java.io.BufferedWriter;
import java.util.ArrayList;

public class Entrypoint implements HttpFunction {

    private static final String gcpProjectID = "CN2324-T1-G15";

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        BufferedWriter writer = response.getWriter();
        String instanceGroup =
                request.getFirstQueryParameter("instanceGroup").orElse("instance-group-server");

        ArrayList<String> ipAddresses = new ArrayList<>();
        try (InstancesClient client = InstancesClient.create()) {
            for (Instance instance : client.list(gcpProjectID, zone).iterateAll()) {
                if (instance.getStatus().compareTo("RUNNING") == 0) {
                    if (instance.getName().contains(instanceGroup)) {
                        String ip = instance.getNetworkInterfaces(0).getAccessConfigs(0).getNatIP();
                        ipAddresses.add(ip);
                    }
                }
            }
        }
        writer.write(new Gson().toJson(ipAddresses.toArray()));
    }
}

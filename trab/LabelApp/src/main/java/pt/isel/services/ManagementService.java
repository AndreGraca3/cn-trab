package pt.isel.services;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.compute.v1.InstanceGroupManagersClient;
import com.google.cloud.compute.v1.Operation;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import management.InstanceCount;
import management.ManagementServiceGrpc;
import management.PingResponse;
import management.RequestTimestamp;

import java.time.Duration;
import java.time.LocalDateTime;

public class ManagementService extends ManagementServiceGrpc.ManagementServiceImplBase {


    @Override
    public void isAlive(RequestTimestamp request, StreamObserver<PingResponse> responseObserver) {
        LocalDateTime startTime = LocalDateTime.parse(request.getTimestamp());
        var ping = (int) Duration.between(startTime, LocalDateTime.now()).toMillis();

        responseObserver.onNext(PingResponse.newBuilder().setPing(ping).build());
        responseObserver.onCompleted();
    }


    @Override
    public void changeImageProcessingInstances(InstanceCount request, StreamObserver<Empty> responseObserver) {
        String projectID = "cn-g15";
        String zone = "europe-southwest1-a";
        String instanceGroupName = "grpc-Label-group";
        int count = request.getCount();


        try{
            InstanceGroupManagersClient managersClient = InstanceGroupManagersClient.create();


            int newSize = Math.max(1, count);

            OperationFuture<Operation, Operation> result = managersClient.resizeAsync(
                    projectID,
                    zone,
                    instanceGroupName,
                    newSize
            );
            Operation oper = result.get();
            System.out.println("Resizing with status " + oper.getStatus());

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

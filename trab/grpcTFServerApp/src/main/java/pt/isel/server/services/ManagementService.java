package pt.isel.server.services;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.compute.v1.InstanceGroupManagersClient;
import com.google.cloud.compute.v1.Operation;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import management.InstanceCount;
import management.ManagementServiceGrpc;

import java.io.IOException;

public class ManagementService extends ManagementServiceGrpc.ManagementServiceImplBase {

    InstanceGroupManagersClient managersClient;

    public ManagementService(int svcPort) throws IOException {
        managersClient = InstanceGroupManagersClient.create();
        System.out.println("Management Service is available on port:" + svcPort);
    }

    @Override
    public void isAlive(Empty request, StreamObserver<Empty> responseObserver) {
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void changeGRPCServerInstances(InstanceCount request, StreamObserver<Empty> responseObserver) {
        String projectID = "cn2324-t1-g15";
        String zone = "europe-southwest1-a";
        String instanceGroupName = "instance-group-server";
        int count = request.getCount();

        try {
            if (count < 1) {
                responseObserver.onError(new IllegalArgumentException("Count must be a positive number"));
                return;
            }

            OperationFuture<Operation, Operation> result = managersClient.resizeAsync(
                    projectID,
                    zone,
                    instanceGroupName,
                    count
            );
            Operation oper = result.get();
            System.out.println("Resizing with status " + oper.getStatus());

            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void changeImageProcessingInstances(InstanceCount request, StreamObserver<Empty> responseObserver) {
        String projectID = "cn2324-t1-g15";
        String zone = "europe-southwest1-a";
        String instanceGroupName = "instance-group-label";
        int count = request.getCount();

        try {
            if (count < 0) {
                responseObserver.onError(new IllegalArgumentException("Count must be a positive number"));
                return;
            }

            OperationFuture<Operation, Operation> result = managersClient.resizeAsync(
                    projectID,
                    zone,
                    instanceGroupName,
                    count
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

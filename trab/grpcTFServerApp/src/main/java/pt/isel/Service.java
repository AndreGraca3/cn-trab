package pt.isel;

import label.ServiceGrpc;

public class Service extends ServiceGrpc.ServiceImplBase {

    public Service(int svcPort) {
        System.out.println("Service is available on port:" + svcPort);
    }

}

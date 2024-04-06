package grpcclientapp;

import io.grpc.stub.StreamObserver;
import servicestubs.IntNumber;

public class PrimeNumbersStream implements StreamObserver<IntNumber> {
    boolean completed=false;

    @Override
    public void onNext(IntNumber reply) {
        System.out.println("Another prime:"+reply.getIntnumber());
    }
    @Override
    public void onError(Throwable throwable) {
        System.out.println("Completed with error:"+throwable.getMessage());
        completed=true;
    }
    @Override
    public void onCompleted() {
        System.out.println("Find primes completed");
        completed=true;
    }

    public boolean isCompleted() {
        return completed;
    }
}

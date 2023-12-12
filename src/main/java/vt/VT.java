package vt;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class VT {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        new Thread(); // Thread.ofPlatform().unstarted(task);

        Thread.ofPlatform();
        Thread.ofVirtual();

//        new Thread.Builder.OfPlatform();
//        new Thread.Builder.OfVirtual();
    }
}

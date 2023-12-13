package vt;

import java.util.concurrent.ExecutionException;

public class Example21 {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Thread.Builder builder = Thread.ofVirtual()
            .name("MyThread")
            .inheritInheritableThreadLocals(false)
            .uncaughtExceptionHandler((t, e) -> {

            });
        builder.start(() -> System.out.println("run"));
        //builder.unstarted(() -> System.out.println("dont run yet"));

        //name
        //inheritInheritableThreadLocals
        //uncaughtExceptionHandler

//        Thread unstarted(Runnable task);
//        Thread start(Runnable task);

        Thread thread = builder.start(() -> System.out.println("Hello"));
        //assertEquals
        thread.join();
    }
}

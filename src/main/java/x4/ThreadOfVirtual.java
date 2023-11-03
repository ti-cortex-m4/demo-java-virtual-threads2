package x4;

public class ThreadOfVirtual {

    public static void main(String[] args) throws InterruptedException {
        Runnable runnable = () -> {
            System.out.println("run");
        };

        Thread thread1 = Thread.ofVirtual().start(runnable);
        thread1.join();

        Thread thread2 = Thread.ofVirtual().unstarted(runnable);
        thread2.start();
        thread2.join();
    }
}

package virtual_threads;

public class Rule3UseSemaphoresInsteadOfFixedThreadPoolsToLimitConcurrencyTest {

//    ExecutorService es = Executors.newFixedThreadPool(10);
//...
//    Result foo() {
//        try {
//            var fut = es.submit(() -> callLimitedService());
//            return f.get();
//        } catch (...) { ... }
//    }
//
//    Semaphore sem = new Semaphore(10);
//...
//    Result foo() {
//        sem.acquire();
//        try {
//            return callLimitedService();
//        } finally {
//            sem.release();
//        }
//    }

}

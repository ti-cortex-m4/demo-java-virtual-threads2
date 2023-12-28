
## Introduction

Java virtual threads are lightweight threads designed for more efficient use of operating system threads when performing blocking operations. Virtual threads enable a competitive “one thread per blocking call” concurrent model that is easier for programmers than _futures/promises_ and _reactive streams_. Virtual threads support the APIs and semantics of pre-existing threads. To effectively use virtual threads, programmes need to follow guidelines related to the details of their implementation.

Virtual threads were added in Java 19 as a preview feature and released in Java 21.


## Platform threads and virtual threads

A _thread of execution_ is the smallest unit of processing that can be scheduled that runs concurrently with other such units. A Java virtual machine uses the _Thread_ class to manage threads. There are two kinds of threads, platform threads and virtual threads.

Platform threads are mapped one-to-one to _kernel-mode_ threads scheduled by the operating system, for their entire lifetime. The number of available platform threads is limited to the number of operating system threads.  A typical Java virtual machine may support no more than ten thousands of virtual threads.

>Platform threads are suitable for executing all types of tasks, but their use in long-blocking operations is a waste of limited resources.

Virtual threads are _user-mode_ threads scheduled by the Java virtual machine rather than the operating system. Virtual threads are mappen many-to-many to _kernel-mode_ threads scheduled by the operating system. Many virtual threads employ a few platform threads used as _carrier threads_. A Java virtual machine may support millions of virtual threads.

Locking and I/O operations are examples of operations where a carrier thread may be re-scheduled from one virtual thread to another.

>Virtual threads are suitable for executing tasks that spend most of the time blocked, and are not intended for long-running CPU intensive operations.


## How virtual threads work

The operating system schedules when a platform thread is run. However, the Java runtime schedules when a virtual thread is run. When the Java runtime schedules a virtual thread, it _mounts_ the virtual thread on a platform thread, then the operating system schedules that platform thread as usual. This platform thread is called a _carrier_. When the virtual thread performs a blocking I/O operation (or …), the virtual thread can _unmount_ from its carrier. After a virtual thread unmounts from its carrier, the carrier is free, which means that the Java runtime scheduler can mount a different virtual thread on it.

A virtual thread cannot be unmounted during blocking operations when it is _pinned_ to its carrier. A virtual thread is pinned in the following situations:



* The virtual thread runs code inside a synchronized block or method
* The virtual thread runs a native method or a foreign function

Pinning does not make an application incorrect, but it might hinder its scalability. Try avoiding frequent and long-lived pinning.


## How to use virtual threads

The _Thread_ class has public constructors and the _Thread.Builder_ interface to create threads. For backward compatibility, the public constructors of the _Thread_ class can create only platform threads. Virtual threads are instances of the non-public class _VirtualThread_ which cannot be instantiated directly. Builders that implement subinterfaces _Thread.Builder.OfVirtual_ and _Thread.Builder.OfPlatform_ can create virtual and platform threads. These builders are returned from static factory methods _Thread.ofVirtual()_ and _Thread.ofPlatform()_.

![thread class diagram](/images/thread_class_diagram.png)

There are four ways to create virtual threads:



* the thread _builder_
* the _static factory method_
* the _thread factory_
* the _executor service_

The virtual thread _builder_ allows you to create a virtual thread by specifying the following parameters: name, inheritance of inheritable thread-local valiables flag, uncaught exception handler, _Runnable_ task.


```
Thread.Builder builder = Thread.ofVirtual()
   .name("a virtual thread")
   .inheritInheritableThreadLocals(false)
   .uncaughtExceptionHandler((t, e) -> System.out.printf("Thread %s failed with exception %s", t, e));
System.out.println(builder.getClass().getName()); // java.lang.ThreadBuilders$VirtualThreadBuilder

Thread thread = builder.start(() -> System.out.println("run"));
System.out.println(thread.getClass().getName()); // java.lang.VirtualThread
thread.join();
```


The _static factory method_ allows you to create a virtual thread with default parameters by specifying only a _Runnable_ task.


```
Thread thread = Thread.startVirtualThread(() -> System.out.println("run"));
System.out.println(thread.getClass().getName()); // java.lang.VirtualThread
thread.join();
```


The _thread factory_ that implements the _ThreadFactory_ interface_ _allows you to create virtual threads with parameters, previously specified in a _builder_, by specifying a _Runnable_ task. Note that the thread factory is thread-safe but the builder is not.


```
Thread.Builder builder = Thread.ofVirtual();

ThreadFactory threadFactory = builder.factory();
System.out.println(threadFactory.getClass().getName()); // java.lang.ThreadBuilders$VirtualThreadFactory

Thread thread = threadFactory.newThread(() -> System.out.println("run"));
System.out.println(thread.getClass().getName()); // java.lang.VirtualThread
thread.join();
```


The _executor service_ that implements the _ExecutorService_ interface allows you to execute Runnable and Callable tasks in the unbaunded thread-per-task executor service.


```
try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
   System.out.println(executorService.getClass().getName()); // java.util.concurrent.ThreadPerTaskExecutor

   Future<?> future = executorService.submit(() -> System.out.println("run"));
   future.get();
}
```


code exampes

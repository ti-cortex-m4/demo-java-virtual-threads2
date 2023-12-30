# Introduction to Java virtual threads


## Introduction

Java _virtual threads_ are lightweight threads that are designed to make concurrent applications both simpler and more scalable. Pre-existing Java threads were based on operating system threads, which proved insufficient to meet the demands of modern concurrency. Applications such as databases or web servers should serve millions of concurrent requests, but the Java runtime cannot efficiently handle more than a few thousand. If programmers continue to use threads as the unit of concurrency, they will severely limit the throughput of their applications. Alternatively, they can switch to various _asynchronous_ APIs, which are more difficult to develop, debug and understand, but which do not block operating system threads and therefore provide much better performance.

The main goal of virtual threads is to add user-space threads managed by the Java runtime, which would be used alongside the existing heavyweight, kernel-space threads managed by operating systems. Virtual threads are much more lightweight than kernel-mode threads in memory usage, and the overhead of context switching and blocking among them is close to zero. Programmers can create millions of virtual threads in a single JVM instance, and get better performance using much simpler _synchronous blocking_ code.

<sub>Virtual threads were added in Java 19 as a preview feature and released in Java 21.</sub>


## Platform threads and virtual threads

A _thread of execution_ is the smallest unit of processing that can be scheduled that runs concurrently with other such units. A Java virtual machine uses the _Thread_ class to manage threads. There are two kinds of threads, platform threads and virtual threads.

_Platform threads_ are mapped one-to-one to kernel-mode threads scheduled by the operating system, for their entire lifetime. The number of available platform threads is limited to the number of operating system threads.  A typical Java virtual machine may support no more than a few thousands of virtual threads.

>Platform threads are suitable for executing all types of tasks, but their use in long-blocking operations is a waste of limited resources.

_Virtual threads_ are user-mode threads scheduled by the Java virtual machine rather than the operating system. Virtual threads are mappen many-to-many to kernel-mode threads scheduled by the operating system. A Java virtual machine may support millions of virtual threads.

>Virtual threads are suitable for executing tasks that spend most of the time blocked, and are not intended for long-running CPU intensive operations.

Many virtual threads employ a few platform threads used as _carrier threads_. When the Java runtime schedules a virtual thread, it _mounts_ the virtual thread on a carrier thread. When the virtual thread performs a blocking I/O operation or locking, the virtual thread can _unmount_ from its carrier thread. When the carrier thread is free, the Java runtime scheduler can mount a different virtual thread on it.

<sub>Virtual threads have no access to their carrier threads.</sub>

A virtual thread cannot be unmounted during blocking operations when it is _pinned_ to its carrier. A virtual thread is pinned in the following situations:



* the virtual thread runs code inside a _synchronized_ block or method
* the virtual thread runs a _native method_ or a _foreign function_

Pinning does not make an application incorrect, but frequent and long-lived pinning might hinder its scalability.


## How to use virtual threads

The _Thread_ class has public constructors and the inner _Thread.Builder_ interface to create threads. For backward compatibility, the public constructors of the _Thread_ class can create only platform threads. Virtual threads are instances of the non-public class _VirtualThread_ which cannot be instantiated directly. So to create virtual threads you should use a builder with the _Thread.Builder.OfVirtual_ subinterface. To create platform threads you should use a similar builder with the _Thread.Builder.OfPlatform_ subinterface. These builders are returned from static factory methods _Thread.ofVirtual()_ and _Thread.ofPlatform()_ respectively.

![thread class diagram](/images/thread_class_diagram.png)

There are four ways to create and use virtual threads:



* the _thread builder_
* the _static factory method_
* the _thread factory_
* the _executor service_

The virtual _thread builder_ allows you to create a virtual thread with all their available parameters: name, _inheritable-thread-local valiables_ inheritance flag, uncaught exception handler, and _Runnable_ task.


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


<sub>For the platform thread builder you can specify additional parameters: thread group, <em>daemon</em> flag, priority and stack size.</sub>

The _static factory method_ allows you to create a virtual thread with default parameters by specifying only a _Runnable_ task. (Note that by default, the virtual thread name is empty).


```
Thread thread = Thread.startVirtualThread(() -> System.out.println("run"));
System.out.println(thread.getClass().getName()); // java.lang.VirtualThread
thread.join();
```


The _thread factory_ allows you to create virtual threads by specifying a _Runnable_ task to the instance of the _ThreadFactory_ interface. The parameters of virtual threads are determined by the current state of the еркуфв builder from which this factor is created. (Note that the thread factory is thread-safe and the thread builder is not).


```
Thread.Builder builder = Thread.ofVirtual();

ThreadFactory threadFactory = builder.factory();
System.out.println(threadFactory.getClass().getName()); // java.lang.ThreadBuilders$VirtualThreadFactory

Thread thread = threadFactory.newThread(() -> System.out.println("run"));
System.out.println(thread.getClass().getName()); // java.lang.VirtualThread
thread.join();
```


The _executor service_ allows you to execute _Runnable_ and _Callable_ tasks in the unbounded, thread-per-task instance of the _ExecutorService_ interface.


```
try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
   System.out.println(executorService.getClass().getName()); // java.util.concurrent.ThreadPerTaskExecutor

   Future<?> future = executorService.submit(() -> System.out.println("run"));
   future.get();
}
```


code examples

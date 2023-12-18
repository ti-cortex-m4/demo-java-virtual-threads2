<!-----



Conversion time: 1.012 seconds.


Using this Markdown file:

1. Paste this output into your source file.
2. See the notes and action items below regarding this conversion run.
3. Check the rendered output (headings, lists, code blocks, tables) for proper
   formatting and use a linkchecker before you publish this page.

Conversion notes:

* Docs to Markdown version 1.0β35
* Mon Dec 18 2023 11:03:42 GMT-0800 (PST)
* Source doc: Google Translate
* This is a partial selection. Check to make sure intra-doc links work.
----->



#### Creating virtual threads

The constructors of the _Thread_ class and its subclasses can only create platform threads.The sealed _Thread.Builder_ interface provides greater ability to create threads than the constructors. The _Thread.Builder.OfPlatform_ subinterface could create platform threads, and the _Thread.Builder.OfVirtual_ subinterface could create virtual threads.


###### Creating platform threads from constructors

The following example creates a platform thread from a constructor.


```
Thread thread = new Thread(() -> System.out.println("run!"));
assertTrue(thread.isPlatform());
assertFalse(thread.isVirtual());
thread.join();
```



###### Creating platform and virtual threads from builders

The following example creates a platform thread from an implementation of the _Thread.Builder.OfPlatform_ interface that is returned from the _Thread.ofPlatform()_ method.


```
Thread thread = Thread.ofPlatform().start(() -> System.out.println("run"));
assertTrue(thread.isPlatform());
thread.join();
```


The following example creates a virtual thread from an implementation of the _Thread.Builder.ofVirtual_ interface that is returned from the _Thread.ofVirtual()_ method.


```
Thread thread = Thread.ofVirtual().start(() -> System.out.println("run"));
assertTrue(thread.isVirtual());
thread.join();
```


The following example creates a virtual thread and implicitly starts it in the builder.


```
Thread.Builder builder = Thread.ofVirtual();
Thread thread = builder.start(() -> { sleep(1000); System.out.println("run"); });
assertEquals(Thread.State.RUNNABLE, thread.getState());
thread.join();
```


The following example creates a virtual thread and then explicitly starts it.


```
Thread.Builder builder = Thread.ofVirtual();
Thread thread = builder.unstarted(() -> System.out.println("run"));
assertEquals(Thread.State.NEW, thread.getState());
thread.start();
assertEquals(Thread.State.RUNNABLE, thread.getState());
thread.join();
```


The following example creates a platform thread with all the options available in the _Thread.Builder.ofPlatform_ thread builder interface.


```
Thread.Builder builder = Thread.ofPlatform()
   .group(Thread.currentThread().getThreadGroup())
   .daemon(false)
   .priority(10)
   .stackSize(1024)
   .name("platform thread")
   .inheritInheritableThreadLocals(false)
   .uncaughtExceptionHandler((t, e) -> System.out.printf("Thread %s failed with exception %s", t, e));
Thread thread = builder.unstarted(() -> System.out.println("run"));

assertEquals("platform thread", thread.getName());
assertEquals("main", thread.getThreadGroup().getName());
assertFalse(thread.isDaemon());
assertEquals(10, thread.getPriority());
```


The following example creates a virtual thread with all the options available in the _Thread.Builder.OfVirtual_ thread builder interface.


```
Thread.Builder builder = Thread.ofVirtual()
   .name("virtual thread")
   .inheritInheritableThreadLocals(false)
   .uncaughtExceptionHandler((t, e) -> System.out.printf("Thread %s failed with exception %s", t, e));
Thread thread = builder.unstarted(() -> System.out.println("run"));

assertEquals("virtual thread", thread.getName());
assertEquals("VirtualThreads", thread.getThreadGroup().getName());
assertTrue(thread.isDaemon());
assertEquals(5, thread.getPriority());
```


The previous example shows that the following parameters cannot be specified when creating virtual threads:



* thread group (always "VirtualThreads")
* daemon (always _true_)
* priority(always 5)
* stack size


###### Creating virtual threads from thread factory

Builders are the best alternative to constructors (see "Effective Java" 3rd edition by Joshua Bloch, items 1, 2). Interface _Thread.Builder_ was added in Java 21 and is not yet widely used. Much wider use is made of the _ThreadFactory_ interface, which was added in Java 1.5. The _Thread.Builder.factory_ interface method returns a thread-safe _ThreadFactory_ instance to create threads from the current state of the builder.

The following example creates a virtual thread from a factory created from the current state of the builder.


```
Thread.Builder builder = Thread.ofVirtual()
   .name("virtual thread");
ThreadFactory threadFactory = builder.factory();
Thread thread = threadFactory.newThread(() -> System.out.println("run"));

assertEquals("virtual thread", thread.getName());
assertEquals(Thread.State.NEW, thread.getState());

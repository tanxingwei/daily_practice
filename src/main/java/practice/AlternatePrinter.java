package practice;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 题目描述:
 * 使用两个线程交替打印AB
 * 下面分别是synchronized, ReentrantLock Semaphore的实现方案<br>
 *
 * 编写过程中算是踩了两个坑<br>
 * 第一个是开始写synchronized案例的时候,没有把 Object.wait 方法放在 synchronized 代码块中执行导致 IllegalMonitorStateException<br>
 * 具体的内容请见:<a href="https://www.cnblogs.com/myseries/p/13903051.html">博客</a><br>
 *
 * 第二个则是编写LOCKTest案例的时候,线程一直不结束, 然后通过debugger发现打印B的线程一直阻塞.<br>
 * 最后的处理方式则是在finishThreads方法中, 对condition做了一个唤醒操作
 */
public class AlternatePrinter {
    static class SynchronizedTest {
        private static final Object object = new Object();
        public static AtomicInteger atomicInteger = new AtomicInteger(100);
        private static volatile boolean flag = true;

        private static boolean isFinishPrint(AtomicInteger atomicInteger) {
            return atomicInteger.intValue() == 0;
        }

        private static void finishThreads() {
            flag = false;
        }

        public static void main(String[] args) {
            new Thread(() -> {
                int i = 0;
                try {
                    object.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                while (flag) {
                    synchronized (object) {
                        if (isFinishPrint(atomicInteger)) {
                            finishThreads();
                            object.notifyAll();
                        }
                        if (atomicInteger.get() <= 100 && atomicInteger.intValue() != 0) {
                            System.out.println("A " + ++i);
                            atomicInteger.decrementAndGet();
                            object.notifyAll();
                            try {
                                object.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }).start();

            new Thread(() -> {
                int i = 0;
                while (flag) {
                    synchronized (object) {
                        if (isFinishPrint(atomicInteger)) {
                            finishThreads();
                            object.notifyAll();
                        }
                        if (atomicInteger.get() <= 100 && atomicInteger.intValue() != 0) {
                            System.out.println("B " + ++i);
                            atomicInteger.decrementAndGet();

                            object.notifyAll();
                            try {
                                object.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }).start();
        }
    }

    static class LockTest {
        private static final ReentrantLock lock = new ReentrantLock();
        private static final Condition c = lock.newCondition();
        public static AtomicInteger atomicInteger = new AtomicInteger(100);
        private static volatile boolean flag = true;

        private static boolean isFinishPrint(AtomicInteger atomicInteger) {
            return atomicInteger.intValue() == 0;
        }

        private static void finishThreads() {
            flag = false;
            c.signal();
        }

        public static void main(String[] args) {
            new Thread(() -> {
                int i = 0;

                while (flag) {
                    lock.lock();
                    try {
                        if (isFinishPrint(atomicInteger)) {
                            finishThreads();
                        }
                        if (atomicInteger.get() <= 100 && atomicInteger.intValue() != 0) {
                            System.out.println("A " + ++i);
                            atomicInteger.decrementAndGet();
                            c.signal();
                            try {
                                c.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } finally {
                        lock.unlock();
                    }
                }
                System.out.println("A 执行结束");

            }).start();

            new Thread(() -> {
                int i = 0;
                while (flag) {
                    lock.lock();
                    try {
                        if (isFinishPrint(atomicInteger)) {
                            finishThreads();
                        }
                        if (atomicInteger.get() < 100 && atomicInteger.intValue() != 0) {
                            System.out.println("B " + ++i);
                            atomicInteger.decrementAndGet();

                            c.signal();
                            try {
                                c.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } finally {
                        lock.unlock();
                    }
                }
                System.out.println("B 执行结束");
            }).start();
        }
    }


    static class SemaphoreTest {
        public static AtomicInteger atomicInteger = new AtomicInteger(100);

        private static final Semaphore semaphoreA = new Semaphore(0);

        private static final Semaphore semaphoreB = new Semaphore(0);

        private static volatile boolean flag = true;


        private static boolean isFinishPrint(AtomicInteger atomicInteger) {
            return atomicInteger.intValue() == 0;
        }

        private static void finishThreads() {
            flag = false;
        }

        public static void main(String[] args) {
            new Thread(() -> {
                int i = 0;

                while (flag) {
                    if (isFinishPrint(atomicInteger)) {
                        finishThreads();
                    }
                    if (atomicInteger.get() <= 100 && atomicInteger.intValue() != 0) {
                        System.out.println("A " + ++i);
                        atomicInteger.decrementAndGet();
                        semaphoreB.release();
                        try {
                            semaphoreA.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();

            new Thread(() -> {
                int i = 0;
                while (flag) {
                    if (isFinishPrint(atomicInteger)) {
                        finishThreads();
                    }
                    if (atomicInteger.get() <= 100 && atomicInteger.intValue() != 0) {
                        System.out.println("B " + ++i);
                        atomicInteger.decrementAndGet();
                        semaphoreA.release();
                        try {
                            semaphoreB.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }
}


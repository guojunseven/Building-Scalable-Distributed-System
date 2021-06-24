package model;

/**
 * This is a counter for count the number of successful requests and unsuccessful requests.
 */
public class RequestsCount {

    private final Object lock1 = new Object();
    private final Object lock2 = new Object();
    private int succCount = 0;
    private int failCount = 0;

    /**
     * Add one to the successful requests count.
     */
    public void markSucc() {
        synchronized (lock1) {
            succCount++;
        }
    }

    /**
     * Add one to the unsuccessful requests count.
     */
    public void markFail() {
        synchronized (lock2) {
            failCount++;
        }
    }

    /**
     * Get the number of successful requests.
     * @return the succCount
     */
    public synchronized int getSuccCount() {
        return succCount;
    }

    /**
     * Get the number of unsuccessful requests.
     * @return the failCount
     */
    public synchronized int getFailCount() {
        return failCount;
    }
}

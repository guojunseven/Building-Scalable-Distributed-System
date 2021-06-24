package client;

import io.swagger.client.*;
import io.swagger.client.api.TextbodyApi;
import io.swagger.client.model.*;
import statistic.StatisticsRunnable;

import java.io.BufferedWriter;
import java.util.concurrent.*;

/**
 * This class represents a single client thread to send requests to target server.
 */
public class ApiClientThread extends Thread {

    private final BlockingQueue<String> workQueue;
    private final TextbodyApi apiInstance;
    private final String function;
    private final CyclicBarrier synk;
    private final RequestsCount counter;
    private final String end;
    private final ThreadPoolExecutor pool;
    private final BufferedWriter writer;

    /**
     * Initialize a client thread with a reader which will be accessed
     * exclusively.
     * @param workQueue the reader to read lines of text.
     * @param basePath the path to send http requests.
     * @param function the function name to process the line of text.
     * @param barrier the cyclic barrier to make threads coordinated.
     * @param counter the counter to count the number of requests.
     */
    public ApiClientThread(
            BlockingQueue<String> workQueue, String basePath, String function,
            CyclicBarrier barrier, RequestsCount counter, String end, ThreadPoolExecutor pool, BufferedWriter writer) {
        if (workQueue == null
                || basePath == null
                || function == null
                || barrier == null
                || counter == null
                || end == null
                || pool == null
                || writer == null) {
            throw new IllegalArgumentException("invalid thread parameters");
        }
        this.workQueue = workQueue;
        this.apiInstance = new TextbodyApi(new ApiClient().setBasePath(basePath));
        this.function = function;
        this.synk = barrier;
        this.counter = counter;
        this.end = end;
        this.pool = pool;
        this.writer = writer;
    }

    public void run() {
        // continuously read line from reader and sending http request
        TextLine body = new TextLine();
        String line = "";
        while (true) {
            try {
                line = workQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // reach the end of the file, break and wait other threads to complete
            if (line == end) {
                break;
            }
            // check if it is an empty line
            if (line.length() == 0) {
                continue;
            }
            // take a timestamp before sending a request
            int responseCode = 200;
            long start = System.nanoTime();
            try {
                apiInstance.analyzeNewLine(body.message(line), function);
                counter.markSucc();
                // System.out.println(result);
            } catch (ApiException e) {
                counter.markFail();
                System.err.println(String.format("errCode : %d, %s", e.getCode(), e.getMessage()));
                e.printStackTrace();
                responseCode = e.getCode();
            }
            long latency = System.nanoTime() - start;
            pool.execute(new StatisticsRunnable(writer, new String[]{"" + start, "POST", "" + latency, "" + responseCode}));
        }

        // wait for other thread to complete
        try {
            synk.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }
}

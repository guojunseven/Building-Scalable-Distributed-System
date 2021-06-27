package client;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.TextbodyApi;
import io.swagger.client.model.TextLine;
import model.Signal;
import statistic.StatisticsRunnable;

import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * This class is for sending GET requests to target URL.
 */
public class ApiGetThread extends Thread {

    private final TextbodyApi apiInstance;
    private final String function;
    private final CyclicBarrier synk;
    private final RequestsCount counter;
    private final Signal sign;
    private final ThreadPoolExecutor pool;
    private final BufferedWriter writer;
    private final String[] wordList = new String[]{"balancer", "guojun", "response", "partition", "distributed", "system", "internet",
    "database", "scalable", "lock"};

    /**
     * Initialize a client thread with a reader which will be accessed
     * exclusively.
     * @param basePath the path to send http requests.
     * @param function the function name to process the line of text.
     * @param barrier the cyclic barrier to make threads coordinated.
     * @param sign the sign to notify this thread to stop
     * @param counter the counter to count the number of requests.
     */
    public ApiGetThread(String basePath, String function, CyclicBarrier barrier,
                        RequestsCount counter, Signal sign, ThreadPoolExecutor pool, BufferedWriter writer) {
        if (     basePath == null
                || function == null
                || barrier == null
                || counter == null
                || pool == null
                || writer == null) {
            throw new IllegalArgumentException("invalid thread parameters");
        }
        this.apiInstance = new TextbodyApi(new ApiClient().setBasePath(basePath));
        this.function = function;
        this.synk = barrier;
        this.counter = counter;
        this.sign = sign;
        this.pool = pool;
        this.writer = writer;
    }

    public void run() {
        while (true) {
            // check if need to terminate
            if (sign.get()) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (String word : wordList) {
                // take a timestamp before sending a request
                int responseCode = 200;
                long start = System.nanoTime();
                try {
                    apiInstance.getWordCount(word);
                    counter.markSucc();
                    // System.out.println(result);
                } catch (ApiException e) {
                    counter.markFail();
                    responseCode = e.getCode();
                }
                long latency = System.nanoTime() - start;
                pool.execute(new StatisticsRunnable(writer, new String[]{"" + start, "GET", "" + latency, "" + responseCode}));
            }
        }

        // wait for other thread to complete
        try {
            synk.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }
}

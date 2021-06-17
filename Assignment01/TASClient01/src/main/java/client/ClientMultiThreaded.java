package client;

import java.io.*;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * This class generates multiple api client threads to read line from a given input
 * and send them to server for analysis. The main thread will wait for all threads to complete.
 */
public class ClientMultiThreaded {

    private static String input;
    private static int maxThreads;
    private static String localPath = "http://localhost:8080/TextProcessor";
    private static String publicPath = "http://localhost:8080/gortonator/TextProcessor/1.0.2";
    private static String lbs = "http://lbs6650-1874071894.us-east-1.elb.amazonaws.com:8080/TextProcessor";

    private static String basePath = "http://54.91.113.78:8080/TextProcessor";
    private static String function = "wordcount";

    /**
     * This main method takes two parameters: one is the path of text input file the other is the MAX_Threads
     * used to process the text.
     * @param args the input file and max_threads
     */
    public static void main(String[] args) throws IOException, InterruptedException, BrokenBarrierException {
        // check the arguments
        checkArgs(args);

        InputStream is = ClientMultiThreaded.class.getClassLoader().getResourceAsStream(input);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        RequestsCount counter = new RequestsCount();

        // using a blocking queue to distribute lines of text
        BlockingQueue<String> workQueue = new ArrayBlockingQueue<>(12000);

        // creating barrier
        CyclicBarrier barrier = new CyclicBarrier(maxThreads + 1);
        String end = "EOF";

        // Thread to read line and put the line to the work queue
        CyclicBarrier synk = new CyclicBarrier(2);
        new TextReaderThread(workQueue, reader, end, maxThreads, synk).start();
        synk.await();


        long start = System.currentTimeMillis();

        for (int i = 0; i < maxThreads; i++) {
            new ApiClientThread(workQueue, localPath, function, barrier, counter, end).start();
        }

        barrier.await();
        reader.close();

        long wallTime = System.currentTimeMillis() - start;

        System.out.println(String.format("All %d threads processing completed!", maxThreads));
        System.out.println("-----------------------------------------------------");
        System.out.println("------------------------STATS------------------------");
        System.out.println(String.format("Total number of successful requests: %d", counter.getSuccCount()));
        System.out.println(String.format("Total number of unsuccessful requests: %d", counter.getFailCount()));
        System.out.println(String.format("Total wall time: %d ms", wallTime));
        System.out.println(String.format("Throughput: %.2f",
                1000 * (counter.getSuccCount() + counter.getFailCount()) * 1D / wallTime));
        System.out.println("-----------------------------------------------------");
    }

    /**
     * Check the argument passed by Command Line. It will throw Exception when arguments are invalid.
     * @param args the command line arguments
     */
    private static void checkArgs(String args[]) {
        Objects.requireNonNull(args);
        // validate and initialize the parameters
        if (args.length != 2) {
            throw new IllegalArgumentException("wrong arguments number");
        } else {
            input = args[0];
            if (input == null || input.isEmpty()) {
                throw new IllegalArgumentException("input file name can not be null or empty");
            }

            String maxThreadsString = args[1];
            if (maxThreadsString == null || maxThreadsString.isEmpty()) {
                throw new IllegalArgumentException("max threads number can not be null or empty");
            }

            try {
                maxThreads = Integer.parseInt(maxThreadsString);
                if (maxThreads <= 0) {
                    throw new IllegalArgumentException("max threads number must be positive");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("must input a valid integer for max threads number");
            }
        }
    }
}

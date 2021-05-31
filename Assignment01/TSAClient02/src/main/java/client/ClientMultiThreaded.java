package client;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * This class generates multiple api client threads to read line from a given input
 * and send them to server for analysis. The main thread will wait for all threads to complete.
 */
public class ClientMultiThreaded {

    private static String input;
    private static int maxThreads;
    private static String localPath = "http://localhost:8080/TextProcessor";
    private static String basePath = "http://ec2-54-91-96-97.compute-1.amazonaws.com:8080/TextProcessor";
    private static String function = "wordcount";
    private static String csvPath = "statistics.csv";

    /**
     * This main method takes two parameters: one is the path of text input file the other is the MAX_Threads
     * used to process the text.
     * @param args the input file and max_threads
     */
    public static void main(String[] args) throws IOException, InterruptedException, BrokenBarrierException {
        checkArgs(args);

        // read the text to a buffered reader
        InputStream is = ClientMultiThreaded.class.getClassLoader().getResourceAsStream(input);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        // count the requests
        RequestsCount counter = new RequestsCount();

        // using a blocking queue to distribute lines of text
        BlockingQueue<String> workQueue = new ArrayBlockingQueue<>(maxThreads);
        CyclicBarrier barrier = new CyclicBarrier(maxThreads + 1);
        String end = "EOF";

        // Thread to read line and put the line to the work queue
        new TextReaderThread(workQueue, reader, end, maxThreads).start();

        // Thread pool to write statistics to csv file
        BufferedWriter writer = new BufferedWriter(new FileWriter(csvPath));
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                10, 10, 50, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

        long start = System.currentTimeMillis();
        for (int i = 0; i < maxThreads; i++) {
            new ApiClientThread(workQueue, basePath, function, barrier, counter, end, pool, writer).start();
        }

        // wait for threads to complete
        barrier.await();

        long wallTime = System.currentTimeMillis() - start;

        // wait for writing csv
        while (true) {
            if (pool.getActiveCount() == 0) {
                pool.shutdown();
                break;
            }
        }

        // analyze the statistics
        writer.flush();
        writer.close();
        // analyze the statistics
        String statistics = analyze();

        System.out.println(String.format("All %d threads processing completed!", maxThreads));
        System.out.println("-----------------------------------------------------");
        System.out.println("------------------------STATS------------------------");
        System.out.println(String.format("Total number of successful requests: %d", counter.getSuccCount()));
        System.out.println(String.format("Total number of unsuccessful requests: %d", counter.getFailCount()));
        System.out.println(statistics);
        System.out.println(String.format("Total wall time: %d ms", wallTime));
        System.out.println(String.format("Throughput: %.2f",
                1000 * (counter.getSuccCount() + counter.getFailCount()) * 1D / wallTime));
        System.out.println("-----------------------------------------------------");
    }

    /**
     * Analyze the statistics CSV file and return the core information.
     * @return statistics report
     */
    private static String analyze() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(csvPath));
        List<Double> latencies = new ArrayList<Double>();
        String line;

        double sum = 0;
        double max = 0;
        while ((line = reader.readLine()) != null) {
            double latency = Double.parseDouble(line.split(",")[2]) / 1000000;
            latencies.add(latency);
            sum += latency;
            max = Math.max(max, latency);
        }
        int len = latencies.size();

        Collections.sort(latencies, Collections.reverseOrder());
        double percentile99 = latencies.get((int) Math.floor(1D * len / 100));
        double mean = sum / len;
        double median = len % 2 == 0 ? (latencies.get(len / 2) + latencies.get(len / 2 - 1)) / 2 : latencies.get(len / 2);
        StringBuilder statistics = new StringBuilder();
        statistics.append(String.format("Mean response time : %.4f ms", mean));
        statistics.append("\n");
        statistics.append(String.format("Median response time : %.4f ms", median));
        statistics.append("\n");
        statistics.append(String.format("99th Percentile response time : %.4f ms", percentile99));
        statistics.append("\n");
        statistics.append(String.format("Max response time : %.4f ms", max));
        return statistics.toString();
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

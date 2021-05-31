package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

/**
 * This is a thread for reading text from a File line by line.
 */
public class TextReaderThread extends Thread {

    private final BufferedReader reader;
    private final BlockingQueue<String> workQueue;
    private final String end;
    private final int maxThreads;

    /**
     * Initialize the thread.
     * @param workQueue workQueue
     * @param reader File reader
     * @param end mark the end of the work queue
     * @param maxThreads the number of the end mark
     *
     */
    public TextReaderThread(BlockingQueue<String> workQueue, BufferedReader reader, String end, int maxThreads) {
        Objects.requireNonNull(workQueue);
        Objects.requireNonNull(reader);
        Objects.requireNonNull(end);
        this.workQueue = workQueue;
        this.reader = reader;
        this.end = end;
        this.maxThreads = maxThreads;
    }

    @Override
    public void run() {
        // put lines of text to the queue one by one
        while (true) {
            try {
                String line = reader.readLine();
                if (line == null) break;
                workQueue.put(line);
            } catch (IOException | InterruptedException e) {
                System.err.println("Error when reading file");
                e.printStackTrace();
            }
        }

        // it serves as notifying the requests-sending threads that there is no more text
        // they could just terminate
        for (int i = 0; i < maxThreads; i++) {
            try {
                workQueue.put(end);
            } catch (InterruptedException e) {
                System.err.println("Error when reading file");
            }
        }
    }
}

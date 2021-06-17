package statistic;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Objects;

/**
 * This class represents a thread object working for write statistical data of http client requests
 * to a specified csv file.
 */
public class StatisticsRunnable implements Runnable {

    private final BufferedWriter writer;
    private final String[] lineData;

    public StatisticsRunnable(BufferedWriter writer, String[] lineData) {
        Objects.requireNonNull(writer);
        this.writer = writer;
        this.lineData = lineData;
    }

    @Override
    public void run() {
        try {
            writer.write(String.join(",", lineData) + "\n");
        } catch (IOException e) {
            System.err.println("fail to write statistical info to csv file");
        }
    }
}

package consumer;

import com.rabbitmq.client.*;
import com.rabbitmq.client.ConnectionFactory;
import model.JDBCDataSource;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeoutException;

/**
 * This is a consumer that can consume the rabbit in multiple threads, ie multiple consumers.
 */
public class MultiThreadedConsumerv2 {
    private static String queueName = "wordCount";
    private static int maxThreads;

    public static void main(String[] args) throws IOException {
        if (args == null || args.length == 0) {
            System.err.println("must provide threads number");
        } else {
            maxThreads = Integer.parseInt(args[0]);
        }

        // set properties
        Properties properties = new Properties();
        properties.load(MultiThreadedConsumerv2.class.getClassLoader().getResourceAsStream("application.properties"));

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(properties.getProperty("hostname"));
        factory.setUsername(properties.getProperty("username"));
        factory.setPassword(properties.getProperty("password"));

        // set Mysql connection
        JDBCDataSource datasource = new JDBCDataSource();

        CyclicBarrier synk = new CyclicBarrier(maxThreads + 1);
        try {
            Connection connection = factory.newConnection();
            for (int i = 0; i < maxThreads; i++) {
                Channel channel = connection.createChannel();
                channel.basicQos(1);
                channel.queueDeclare(queueName, false, false, false, null);

                new ConsumerHandlerv2(datasource, channel, queueName, synk).start();
            }
            synk.await();
        } catch (IOException | TimeoutException e) {
            System.err.println("fail to create rabbitmq connection");
            e.printStackTrace();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
    }



}

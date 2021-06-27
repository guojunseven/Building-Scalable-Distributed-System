package consumer;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeoutException;

/**
 * This is a consumer that can consume the rabbit in multiple threads, ie multiple consumers.
 */
public class MultiThreadedConsumer {
    private static String queueName = "wordCount";
    private static int maxThreads;

    public static void main(String[] args) throws IOException {
        if (args == null || args.length == 0) {
            System.err.println("must provide threads number");
        } else {
            maxThreads = Integer.parseInt(args[0]);
        }

        // set rabbitmq properties
        Properties properties = new Properties();
        properties.load(MultiThreadedConsumer.class.getClassLoader().getResourceAsStream("application.properties"));

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(properties.getProperty("hostname"));
        factory.setUsername(properties.getProperty("username"));
        factory.setPassword(properties.getProperty("password"));

        // create dynamoDB mapper
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion("us-east-1")
                .build();
        DynamoDBMapper mapper = new DynamoDBMapper(client);

        CyclicBarrier synk = new CyclicBarrier(maxThreads + 1);

        try {
            Connection  connection = factory.newConnection();
            for (int i = 0; i < maxThreads; i++) {
                Channel channel = connection.createChannel();
                channel.queueDeclare(queueName, false, false, false, null);
                new ConsumerHandler(mapper, channel, queueName, synk).start();
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

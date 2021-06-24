package consumer;

import com.rabbitmq.client.*;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * This is a consumer handler for single thread. And it will pull message from the queue
 * and store the key-value pair in the hashmap.
 */
public class consumerHandler extends Thread{

    private final BasicDataSource dataSource;
    private final Channel channel;
    private final String queueName;
    private final CyclicBarrier synk;

    /**
     * Initialize the handler with the target hashmap for storing the word-count and the channel where to pull message.
     * @param dataSource the thread-safe hashmap
     * @param channel channel to pull message
     * @param queueName target queue
     * @param synk the synk barrier
     */
    public consumerHandler(
            BasicDataSource dataSource, Channel channel, String queueName, CyclicBarrier synk) {
        this.dataSource = dataSource;
        this.channel = channel;
        this.queueName = queueName;
        this.synk = synk;
    }

    @Override
    public void run() {
        try {
            Connection conn = dataSource.getConnection(); // create connection and release it
            conn.close();
            System.out.println("end");
        } catch (SQLException e) {
            System.err.println("fail to get connection from data source");
        }

        DefaultConsumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException
            {
                Map<String, Integer> data = SerializationUtils.deserialize(body);
                Iterator<Map.Entry<String, Integer>> iter = data.entrySet().iterator();
                Connection conn = null;

                try {
                    conn = dataSource.getConnection();
                } catch (SQLException throwables) {
                    System.err.println("fail to get connection from data source");
                }

                while (iter.hasNext()) {
                    Map.Entry<String, Integer> entry = iter.next();
                    update(conn, entry.getKey(), entry.getValue());

                }

                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException throwables) {
                        System.out.println("fail to close the connection");
                    }
                }
                long deliveryTag = envelope.getDeliveryTag();
                channel.basicAck(deliveryTag, false);
            }
        };

        try {
            channel.basicConsume(queueName, false, consumer);
            System.out.println("consume");
        } catch (IOException e) {
            System.err.println("fail to consume messages");
        }

        try {
            synk.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }

    private void update(Connection conn, String word, int count) {
        if (conn == null) {
            throw new IllegalArgumentException("can not take a null connection");
        }
        PreparedStatement preparedStatement = null;
        word = word.replace("\"", "\"\"");
        String insertQueryStatement = String.format("INSERT INTO wordCount (word, count) VALUES (\"%s\",%d) " +
                "ON DUPLICATE KEY UPDATE count = count + %d", word, count, count);
        try {
            preparedStatement = conn.prepareStatement(insertQueryStatement);
            // execute insert SQL statement
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("fail to execute this statement :" + e.getMessage());
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
}

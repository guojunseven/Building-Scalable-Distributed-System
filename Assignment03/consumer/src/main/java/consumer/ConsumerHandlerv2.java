package consumer;

import com.rabbitmq.client.*;
import model.JDBCDataSource;
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
public class ConsumerHandlerv2 extends Thread{

    private final JDBCDataSource dataSource;
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
    public ConsumerHandlerv2(
            JDBCDataSource dataSource, Channel channel, String queueName, CyclicBarrier synk) {
        this.dataSource = dataSource;
        this.channel = channel;
        this.queueName = queueName;
        this.synk = synk;
    }

    @Override
    public void run() {
        Connection conn = null;

        String insertQueryStatement = "INSERT INTO wordsCount (word, count) VALUES (?, ?)";

        DefaultConsumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException
            {
                Map<String, Integer> data = SerializationUtils.deserialize(body);
                long deliveryTag = envelope.getDeliveryTag();
                System.out.println(deliveryTag);
                channel.basicAck(deliveryTag, false);

                Iterator<Map.Entry<String, Integer>> iter = data.entrySet().iterator();

                Connection conn = null;
                PreparedStatement preparedStatement = null;
                try {
                    conn = dataSource.getConnetion();
                    preparedStatement = conn.prepareStatement(insertQueryStatement);
                } catch (SQLException e) {
                    e.printStackTrace();
                }


                while (iter.hasNext()) {
                    Map.Entry<String, Integer> entry = iter.next();
                    update(preparedStatement, entry.getKey(), entry.getValue());
                }

                try {
                    conn.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }


                try {
                    preparedStatement.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }

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

    private void update(PreparedStatement preparedStatement, String word, int count) {
//        if (conn == null) {
//            throw new IllegalArgumentException("can not take a null connection");
//        }
//        PreparedStatement preparedStatement = null;

        try {

            preparedStatement.setString(1, word);
            preparedStatement.setInt(2, count);
            // execute insert SQL statement
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("fail to execute this statement :" + e.getMessage());
        }
    }
}

package consumer;

import com.rabbitmq.client.*;
import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;

/**
 * This is a consumer handler for single thread. And it will pull message from the queue
 * and store the key-value pair in the hashmap.
 */
public class consumerHandler extends Thread{

    private final ConcurrentHashMap<String, Integer> map;
    private final Channel channel;
    private final String queueName;
    private final CyclicBarrier synk;

    /**
     * Initialize the handler with the target hashmap for storing the word-count and the channel where to pull message.
     * @param map the thread-safe hashmap
     * @param channel channel to pull message
     * @param queueName target queue
     * @param synk the synk barrier
     */
    public consumerHandler(
            ConcurrentHashMap<String, Integer> map, Channel channel, String queueName, CyclicBarrier synk) {
        this.map = map;
        this.channel = channel;
        this.queueName = queueName;
        this.synk = synk;
    }

    @Override
    public void run() {
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
                while (iter.hasNext()) {
                    Map.Entry<String, Integer> entry = iter.next();
                    String key = entry.getKey();
                    map.put(key, map.getOrDefault(key, 0) + entry.getValue());
                }
                long deliveryTag = envelope.getDeliveryTag();
                channel.basicAck(deliveryTag, false);
            }
        };

        try {
            channel.basicConsume(queueName, false, consumer);
        } catch (IOException e) {
            System.err.println("fail to consume messages");
        }

        try {
            synk.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }
}

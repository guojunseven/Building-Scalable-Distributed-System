package consumer;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.rabbitmq.client.*;
import model.WordCount;
import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * This is a consumer handler for single thread. And it will pull message from the queue
 * and store the key-value pair in the hashmap.
 */
public class ConsumerHandler extends Thread{

    private final DynamoDBMapper mapper;
    private final Channel channel;
    private final String queueName;
    private final CyclicBarrier synk;


    /**
     * Initialize the handler with the target hashmap for storing the word-count and the channel where to pull message.
     * @param mapper the dynamo db mapper
     * @param channel channel to pull message
     * @param queueName target queue
     * @param synk the synk barrier
     */
    public ConsumerHandler(
            DynamoDBMapper mapper, Channel channel, String queueName, CyclicBarrier synk) {
        this.mapper = mapper;
        this.channel = channel;
        this.queueName = queueName;
        this.synk = synk;

    }

    @Override
    public void run() {

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            Map<String, Integer> data = SerializationUtils.deserialize(delivery.getBody());

            // acknowledge the message
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            // construct the list for batch write
            Iterator<Map.Entry<String, Integer>> iter = data.entrySet().iterator();
            List<WordCount> list = new ArrayList<WordCount>();

            while (iter.hasNext()) {
                Map.Entry<String, Integer> entry = iter.next();
                if (entry.getKey().isEmpty()) {
                    continue;
                }
                WordCount item = new WordCount();
                item.setWord(entry.getKey());
                item.setCount(entry.getValue());
                list.add(item);
            }
            // do batch write
            update(list);
        };

        try {
            channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {});
        } catch (IOException e) {
            System.err.println("fail to consume messages");
        }
        try {
            synk.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }

    private void update(List<WordCount> list) {
       mapper.batchSave(list);
    }
}

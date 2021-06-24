package service;

import com.rabbitmq.client.Channel;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.pool2.ObjectPool;

import java.util.HashMap;

/**
 * A service class serving as a processor to count the number of words in a Line.
 */
public class WordCountService implements TextProcessor {

    private ObjectPool<Channel> channelObjectPool;
    private String queueName;


    /**
     * Create a WordCountService with a channel pool used to send message to consumers.
     * @param channelObjectPool the channels pool
     */
    public WordCountService(ObjectPool<Channel> channelObjectPool, String queueName) {
        this.channelObjectPool = channelObjectPool;
        this.queueName = queueName;
    }

    @Override
    public int apply(String target) throws Exception {
        HashMap<String, Integer> wordCount = new HashMap<String, Integer>();
        String[] words = target.split("\\s+");
        for (String word : words) {
            wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
        }

        Channel channel = channelObjectPool.borrowObject();
        channel.basicPublish("", queueName, null, SerializationUtils.serialize(wordCount));
        // return the channel to the pool
        channelObjectPool.returnObject(channel);
        return wordCount.size();
    }

    @Override
    public boolean validate(String target) {
        return target != null && ! target.isEmpty();
    }
}

package model;

import com.rabbitmq.client.Channel;
import org.apache.commons.pool2.BaseObjectPool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * It represents a user-defined channel pool for storing rabbitmq channels.
 */
public class ChannelPool extends BaseObjectPool<Channel> {

    BlockingQueue<Channel> channelQueue;

    /**
     * Create a Channel pool and add 50 channels to its pool.
     */
    public ChannelPool(int poolSize) {
        this.channelQueue = new ArrayBlockingQueue<>(poolSize);
    }

    @Override
    public Channel borrowObject() throws Exception {
        return channelQueue.take();
    }

    @Override
    public void returnObject(Channel channel) throws Exception {
        channelQueue.add(channel);
    }

    @Override
    public void invalidateObject(Channel channel) throws Exception {
        return;
    }
}

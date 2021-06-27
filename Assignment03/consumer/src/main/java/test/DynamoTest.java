package test;


import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class DynamoTest {

    public static void main(String[] args) {


        int maxThreads = Integer.parseInt(args[0]);

        CyclicBarrier synk = new CyclicBarrier(maxThreads + 1);
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion("us-east-1")
                .build();
        DynamoDBMapper mapper = new DynamoDBMapper(client);
        long start = System.currentTimeMillis();

        for (int i = 0; i < maxThreads; i++) {
            new Thread(new Run(synk, mapper)).start();
        }
        try {
            synk.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }


        System.out.println("" + (System.currentTimeMillis() - start));
    }


}


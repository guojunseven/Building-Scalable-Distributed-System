package test;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import model.WordCount;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class Run implements Runnable {

    CyclicBarrier synk;
    DynamoDBMapper mapper;
    public Run(CyclicBarrier synk, DynamoDBMapper mapper) {
        this.synk = synk;
        this.mapper = mapper;
    }

    @Override
    public void run() {

        String[] words = new String[] {"<<test1>>", "&($", "syryreest3", "66", "pp", "  *0", "---22", "the"};

        for (int i = 0; i < 1; i++) {
            String word = "guojun";

            Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
            eav.put(":val1", new AttributeValue().withS(word));

            DynamoDBQueryExpression<WordCount> queryExpression = new DynamoDBQueryExpression<WordCount>().withIndexName("word-index")
                    .withKeyConditionExpression("word = :val1")
                    .withExpressionAttributeValues(eav).withConsistentRead(false);

            long start = System.currentTimeMillis();
            List<WordCount> betweenReplies = mapper.query(WordCount.class, queryExpression);
            System.out.println(System.currentTimeMillis() - start);
            int ans = 0;

            for (WordCount wordCount : betweenReplies) {
                System.out.println(String.format("word=%s, count=%d", wordCount.getWord(), wordCount.getCount()));
                ans += wordCount.getCount();
            }
//            System.out.println(ans);
//            WordCount wordCount = new WordCount();
//            wordCount.setWord("guojun");
//            wordCount.setCount(100);
//            mapper.save(wordCount);
            // id field is null at this point


        }

        try {
            synk.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
    }




}

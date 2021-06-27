package model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;

@DynamoDBTable(tableName="wordsCount")
public class WordCount {
    private String id;
    private String word;
    private int count;

    @DynamoDBHashKey(attributeName = "ID")
    @DynamoDBAutoGeneratedKey
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @DynamoDBAttribute(attributeName="word")
    public String getWord() { return this.word; }
    public void setWord(String word) { this.word = word; }

    @DynamoDBAttribute(attributeName="count")
    public int getCount() { return this.count; }
    public void setCount(int count) { this.count = count; }


}
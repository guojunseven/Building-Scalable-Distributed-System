package controller;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.swagger.client.model.TextLine;
import model.ChannelPool;
import model.WordCount;
import org.apache.commons.pool2.ObjectPool;
import service.TextProcessor;
import service.WordCountService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

public class TextProcessServlet extends HttpServlet {

    // this field used for store our various text processor
    private final Map<String, TextProcessor> functions = new HashMap<>();
    private static String queueName = "wordCount";
    private Connection connection;
    private ObjectPool<Channel> channelObjectPool;
    private DynamoDBMapper mapper;
    private Properties properties;

    @Override
    public void init() throws ServletException {
        super.init();
        // set properties
        this.properties = new Properties();
        try {
            properties.load(getServletContext().getResourceAsStream("WEB-INF/application.properties"));
        } catch (IOException e) {
            System.err.println("can not get properties file");
        }
        // set mq connection
        setMqConnection();
        // set channels pool
        setChannelPool();
        // set daynamo database connection
        setDataSource();

        functions.put( "/wordcount",new WordCountService(channelObjectPool, queueName));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json; charset = UTF-8");

        PrintWriter out = response.getWriter();

        String pathVariable = request.getPathInfo();

        if (!isUrlValid(pathVariable)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(new Gson().toJson("This operation is not provided"));

        } else {
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();

            String line = "";
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            TextLine textLine = new Gson().fromJson(sb.toString(), TextLine.class);

            TextProcessor processor = functions.get(pathVariable);

            // check if body exits and if the target is valid
            if (textLine != null && processor.validate(textLine.getMessage())) {
                response.setStatus(HttpServletResponse.SC_OK);
                int result = 0;
                try {
                    result = processor.apply(textLine.getMessage());
                } catch (Exception e) {
                    System.err.println("fail to process the text line message");
                }
                out.write("" + result);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("The input is not valid");
            }
        }
        out.flush();
    }

    /**
     * Check if the url is valid.
     * @param pathVariable the provided url and it is the function name in this case.
     * @return true if the url is valid and vice versa
     */
    private boolean isUrlValid(String pathVariable) {
        return functions.containsKey(pathVariable);
    }

    /**
     * Create one single rabbitmq connection per servlet.
     */
    private void setMqConnection() {
        // create one single rabbitmq connection per servlet
        ConnectionFactory factory = new ConnectionFactory();

        factory.setHost(properties.getProperty("hostname"));
        factory.setUsername(properties.getProperty("username"));
        factory.setPassword(properties.getProperty("password"));

        try {
            this.connection = factory.newConnection();
        } catch (IOException | TimeoutException e) {
            System.err.println("fail to create rabbitmq connection");
            e.printStackTrace();;
        }
    }

    private void setChannelPool() {
        this.channelObjectPool = new ChannelPool(30);
        for (int i = 0; i < 20; i++) {
            try {
                Channel channel = connection.createChannel();
                channel.queueDeclare(queueName, false, false, false, null);
                this.channelObjectPool.returnObject(channel);
            } catch (Exception e) {
                System.err.println("fail to create rabbitmq channel");
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json; charset = UTF-8");

        PrintWriter out = response.getWriter();

        String[] pathVariable = request.getPathInfo().split("/");

        if (!isGetUrlValid(pathVariable)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("This operation is not provided");

        } else {

            int count = 0;

            // construct the query
            Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
            eav.put(":val1", new AttributeValue().withS(pathVariable[2]));

            DynamoDBQueryExpression<WordCount> queryExpression = new DynamoDBQueryExpression<WordCount>()
                    .withIndexName("word-index")
                    .withKeyConditionExpression("word = :val1")
                    .withExpressionAttributeValues(eav).withConsistentRead(false);

            List<WordCount> betweenReplies = mapper.query(WordCount.class, queryExpression);

            for (WordCount wordCount : betweenReplies) {
                count += wordCount.getCount();
            }

            // check if body exits and if the target is valid
            if (count > 0) {
                response.setStatus(HttpServletResponse.SC_OK);
                out.write("" + count);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("the word is not found");
            }
        }
        out.flush();
    }

    /**
     * Check if the url is valid.
     * @param pathVariable the provided url and it is the function name in this case.
     * @return true if the url is valid and vice versa
     */
    private boolean isGetUrlValid(String[] pathVariable) {
        return pathVariable.length == 3 && pathVariable[1].equals("wordcount");
    }

    /**
     * Set the data source of Dynamo
     */
    private void setDataSource() {
        BasicSessionCredentials sessionCredentials = new BasicSessionCredentials(
               properties.getProperty("aws_access_key_id"),
                properties.getProperty("aws_secret_access_key"),
                properties.getProperty("aws_session_token"));
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(sessionCredentials)).withRegion("us-east-1")
                .build();
        this.mapper = new DynamoDBMapper(client);
    }
}

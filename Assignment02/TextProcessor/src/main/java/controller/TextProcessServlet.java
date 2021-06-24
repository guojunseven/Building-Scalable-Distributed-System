package controller;

import com.google.gson.Gson;
import io.swagger.client.model.TextLine;
import model.ChannelPool;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
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
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class TextProcessServlet extends HttpServlet {

    // this field used for store our various text processor
    private final Map<String, TextProcessor> functions = new HashMap<>();
    private static String queueName = "wordCount-persistent";
    private Connection connection;
    private ObjectPool<Channel> channelObjectPool;

    @Override
    public void init() throws ServletException {
        super.init();
        // set mq connection
        setMqConnection();
        // set channels pool
        setChannelPool();
        functions.put( "/wordcount",new WordCountService(channelObjectPool, queueName));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json; charset = UTF-8");

        PrintWriter out = response.getWriter();

        String pathVariable = request.getPathInfo();

        if (!isUrlValid(pathVariable)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);

            out.write(new Gson().toJson(null));

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

                out.write(new Gson().toJson(null));
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
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
        // set properties
        Properties properties = new Properties();
        try {
            properties.load(getServletContext().getResourceAsStream("WEB-INF/application.properties"));
        } catch (IOException e) {
            System.err.println("can not get properties file");
        }
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
        this.channelObjectPool = new ChannelPool(80);
        for (int i = 0; i < 80; i++) {
            try {
                Channel channel = connection.createChannel();
                channel.queueDeclare(queueName, true, false, false, null);
                this.channelObjectPool.returnObject(channel);
            } catch (Exception e) {
                System.err.println("fail to create rabbitmq channel");
            }
        }
    }
}

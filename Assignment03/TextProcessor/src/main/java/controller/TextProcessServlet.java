package controller;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.swagger.client.model.TextLine;
import model.ChannelPool;
import model.JDBCDataSource;
import org.apache.commons.dbcp2.BasicDataSource;
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

public class TextProcessServlet extends HttpServlet {

    // this field used for store our various text processor
    private final Map<String, TextProcessor> functions = new HashMap<>();
    private static String queueName = "wordCount-persistent";
    private Connection connection;
    private ObjectPool<Channel> channelObjectPool;
    private BasicDataSource dataSource;

    @Override
    public void init() throws ServletException {
        super.init();
        // set mq connection
        setMqConnection();
        // set channels pool
        setChannelPool();
        // set database connection
        try {
            setDataSource();
        } catch (IOException e) {
            System.err.println("fail to set data source");
        }
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
        this.channelObjectPool = new ChannelPool(20);
        for (int i = 0; i < 20; i++) {
            try {
                Channel channel = connection.createChannel();
                channel.queueDeclare(queueName, true, false, false, null);
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
            java.sql.Connection conn = null;
            try {
                conn = dataSource.getConnection();
            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println("fail to get mysql connection");
            }

            PreparedStatement preparedStatement = null;
            int count = -1;
            String selectQueryStatement = String.format("select count from wordCount where word = \"%s\"", pathVariable[2]);
            try {
                preparedStatement = conn.prepareStatement(selectQueryStatement);

                // execute insert SQL statement
                ResultSet result = preparedStatement.executeQuery();

                if (result.next()) {
                    count = result.getInt("count");
                }

            } catch (SQLException e) {
                System.err.println("fail to execute this statement :" + selectQueryStatement);
            } finally {
                try {
                    if (preparedStatement != null) {
                        preparedStatement.close();
                    }
                } catch (SQLException se) {
                    se.printStackTrace();
                }
            }
            try {
                conn.close();
            } catch (SQLException throwables) {
                System.err.println("fail to close the mysql connection");
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
     * Set the data source of JDBC
     */
    private void setDataSource() throws IOException {
        Properties properties = new Properties();
        properties.load(getServletContext().getResourceAsStream("WEB-INF/application.properties"));
        dataSource = new JDBCDataSource(properties).getDataSource();
    }
}

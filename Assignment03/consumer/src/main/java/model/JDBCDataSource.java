package model;

import consumer.MultiThreadedConsumer;
import org.apache.commons.dbcp2.BasicDataSource;

import java.io.IOException;
import java.util.Properties;

/**
 * This class servers as a JDBC data source. It is responsible for connecting the database and providing access to the
 * the target data source.
 */
public class JDBCDataSource {

    private BasicDataSource dataSource;

    /**
     * Initialize the data source connection.
     * @throws IOException
     */
    public JDBCDataSource() throws IOException {
        dataSource = new BasicDataSource();
        Properties properties = new Properties();
        properties.load(MultiThreadedConsumer.class.getClassLoader().getResourceAsStream("application.properties"));
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        final String HOST_NAME = properties.getProperty("MySQL_IP_ADDRESS");
        final String PORT = properties.getProperty("MySQL_PORT");
        final String DATABASE = "dev_guojun";
        final String USERNAME = properties.getProperty("DB_USERNAME");
        final String PASSWORD = properties.getProperty("DB_PASSWORD");

        String url = String.format("jdbc:mysql://%s:%s/%s?serverTimezone=UTC", HOST_NAME, PORT, DATABASE);
        dataSource.setUrl(url);
        dataSource.setUsername(USERNAME);
        dataSource.setPassword(PASSWORD);
        dataSource.setInitialSize(20);
        dataSource.setMaxTotal(80);
    }

    /**
     * Return the data source connection
     * @return data source
     */
    public BasicDataSource getDataSource() {
        return dataSource;
    }
}

package model;

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
     * @param properties the properties used to set the connection
     * @throws IOException
     */
    public JDBCDataSource(Properties properties) throws IOException {
        dataSource = new BasicDataSource();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        final String HOST_NAME = System.getProperty("MySQL_IP_ADDRESS");
        final String PORT = System.getProperty("MySQL_PORT");
        final String DATABASE = "dev_guojun";
        final String USERNAME = System.getProperty("DB_USERNAME");
        final String PASSWORD = System.getProperty("DB_PASSWORD");

        String url = String.format("jdbc:mysql://%s:%s/%s?serverTimezone=UTC", HOST_NAME, PORT, DATABASE);
        dataSource.setUrl(url);
        dataSource.setUsername(USERNAME);
        dataSource.setPassword(PASSWORD);
        dataSource.setInitialSize(10);
        dataSource.setMaxTotal(60);
    }

    /**
     * Return the data source connection
     * @return data source
     */
    public BasicDataSource getDataSource() {
        return dataSource;
    }
}

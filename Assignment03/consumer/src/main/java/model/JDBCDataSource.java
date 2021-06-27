package model;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import consumer.MultiThreadedConsumerv2;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * This class servers as a JDBC data source. It is responsible for connecting the database and providing access to the
 * the target data source.
 */
public class JDBCDataSource {

    private HikariConfig config;
    private HikariDataSource ds;

    /**
     * Initialize the data source connection.
     * @throws IOException
     */
    public JDBCDataSource() throws IOException {
        this.config = new HikariConfig();
        Properties properties = new Properties();
        properties.load(MultiThreadedConsumerv2.class.getClassLoader().getResourceAsStream("application.properties"));

        final String HOST_NAME = properties.getProperty("MySQL_IP_ADDRESS");
        final String PORT = properties.getProperty("MySQL_PORT");
        final String DATABASE = "dev_guojun";
        final String USERNAME = properties.getProperty("DB_USERNAME");
        final String PASSWORD = properties.getProperty("DB_PASSWORD");

        String url = String.format("jdbc:mysql://%s:%s/%s?serverTimezone=UTC", HOST_NAME, PORT, DATABASE);
        config.setJdbcUrl(url);
        config.setUsername(USERNAME);
        config.setPassword(PASSWORD);
        config.addDataSourceProperty( "cachePrepStmts" , "true" );
        config.addDataSourceProperty( "prepStmtCacheSize" , "250" );
        config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
        this.ds = new HikariDataSource(config);
        ds.setMaximumPoolSize(60);
    }

    /**
     * Return the data source connection
     * @return data source
     */
    public Connection getConnetion() throws SQLException {
        return ds.getConnection();
    }
}

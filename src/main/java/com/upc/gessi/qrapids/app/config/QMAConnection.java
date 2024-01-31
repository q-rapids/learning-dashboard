package com.upc.gessi.qrapids.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Class that encapsulates the QMA API connection information
 * Opening / Closing the connection with the API is slow.
 * Therefore, we also use this class as a single instance for this connection in order to improve the performance.
 *
 * The first time the operation initConnextion is called, we open the connection with the QMA API.
 * This connection is closed when the instance is removed.
 *
 */
@Component
public class QMAConnection {

    // Attributes for this class, they can be accessed using the getInstance() operation
    private boolean init;

    @Value("${qma.ip}")
    private String ip;

    @Value("${qma.port}")
    private int port;

    @Value("${qma.database.name}")
    private String database;

    @Value("${qma.username}")
    private String user;

    @Value("${qma.password}")
    private String password;

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    // We apply the Singleton pattern, so the constructor is private to avoid instantiation
    private QMAConnection()
    {
        init = false;
    }

    public void initConnexion() {
        if (!this.init) util.Connection.initConnection(ip, port, database, user, password);
        this.init = true;
    }

    protected void finalize() {
        if (this.init) util.Connection.closeConnection();
    }

}

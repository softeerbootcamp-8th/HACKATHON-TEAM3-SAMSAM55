package com.samsam55.trip.global.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.mysql.MySQLContainer;

public abstract class AbstractMySqlContainerTest {

    @ServiceConnection
    static final MySQLContainer MYSQL_CONTAINER = new MySQLContainer("mysql:8.4");

    static {
        MYSQL_CONTAINER.start();
    }
}

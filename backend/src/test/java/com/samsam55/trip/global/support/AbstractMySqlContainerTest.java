package com.samsam55.trip.global.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.mysql.MySQLContainer;

// 실제 MySQL 컨테이너가 필요해서 Docker 없이는 못 돈다. CI(backend-test.yml)가
// "-PexcludeTags=integration"으로 이 태그가 붙은 테스트를 건너뛴다.
@Tag("integration")
public abstract class AbstractMySqlContainerTest {

    @ServiceConnection
    static final MySQLContainer MYSQL_CONTAINER = new MySQLContainer("mysql:8.4");

    static {
        MYSQL_CONTAINER.start();
    }
}

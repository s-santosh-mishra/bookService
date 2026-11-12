package com.servicehub.serv.poc;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddedPostgresPocTest {

    @Test
    void embeddedPostgresStartsConnectsAndShutsDownSuccessfully() throws Exception {
        // Starts on a dynamically allocated port, isolated in a temp directory
        try (EmbeddedPostgres pg = EmbeddedPostgres.builder().start()) {
            int port = pg.getPort();
            assertTrue(port > 0, "Expected dynamically assigned port, got: " + port);

            DataSource dataSource = pg.getPostgresDatabase();
            assertNotNull(dataSource, "DataSource should not be null");

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT version()")) {

                assertTrue(rs.next(), "Expected result from SELECT version()");
                String version = rs.getString(1);

                assertNotNull(version);
                assertTrue(version.contains("PostgreSQL"),
                        "Expected version string to contain 'PostgreSQL', got: " + version);

                System.out.println("==================================================");
                System.out.println("PROOF OF CONCEPT SUCCESSFUL");
                System.out.println("Embedded PostgreSQL Port: " + port);
                System.out.println("PostgreSQL Version Output: " + version);
                System.out.println("==================================================");
            }
        }
        // pg.close() automatically stops postgres.exe and cleans up temp data
    }
}


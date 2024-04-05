/*
 *  Copyright (C) 2024 Piotr Przybył
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package dev.softwaregarden.tcPerfUp;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.esql.jdbc.ResultSetEsqlAdapter;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import dev.softwaregarden.tcPerfUp.misc.DbContainerHelper;
import dev.softwaregarden.tcPerfUp.misc.ElasticsearchContainerHelper;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.lifecycle.Startables;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class BaseIntegrationTest {

    protected static final String MYSQL_IMAGE = "mysql:8.3.0";
    protected static final String ELASTICSEARCH_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:8.13.1";

    protected static final String MYSQL_CONTAINER_BACKUP_LOCATION = "/tmp/schema.sql";

    protected static final String ES_CONTAINER_BACKUP_LOCATION = "/tmp/init_backup.tar.gz";
    protected static final String ES_REPO_LOCATION = "/tmp/bad_backup_location";

    protected static MySQLContainer<?> mySQL = new MySQLContainer<>(MYSQL_IMAGE);

    protected static ElasticsearchContainer elasticsearch =
        new ElasticsearchContainer(ELASTICSEARCH_IMAGE).withEnv("path.repo", ES_REPO_LOCATION);

    protected JacksonJsonpMapper JSONP_MAPPER = new JacksonJsonpMapper();

    static {
        Startables.deepStart(mySQL, elasticsearch).join();
    }

    @BeforeAll
    static void setupContainers() {
        createSnapshotsInContainers();
    }

    @BeforeEach
    void resetContainersState() throws InterruptedException {
        restoreSnapshotsInContainers();
    }

    private static void createSnapshotsInContainers() {
        Thread mt = Thread.ofPlatform().start(() -> {
            DbContainerHelper.runLiquibaseMigrations(mySQL, "config/liquibase/db.changelog-root.xml");
            DbContainerHelper.snapshotMySQL(mySQL, MYSQL_CONTAINER_BACKUP_LOCATION);
        });

        Thread et = Thread.ofPlatform().start(() -> {
            ElasticsearchContainerHelper.prepareData(elasticsearch, "/config/elasticsearch/");
            ElasticsearchContainerHelper.snapshotES(elasticsearch, ES_REPO_LOCATION, ES_CONTAINER_BACKUP_LOCATION);
        });

        try {
            mt.join(20_000);
            et.join(10_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void restoreSnapshotsInContainers() throws InterruptedException {
        Thread mt = Thread.ofPlatform().start(() -> DbContainerHelper.runScript(mySQL, MYSQL_CONTAINER_BACKUP_LOCATION));
        Thread et = Thread.ofPlatform().start(() -> ElasticsearchContainerHelper.restore(elasticsearch));

        mt.join(3_000);
        et.join(1_000);
    }

    /**
     * <img src="https://media.tenor.com/qMQ0nbfy6doAAAAC/theshining-killer.gif" alt="here's Johnny!"/>
     */

    protected void checkJohnnyIsHere(ElasticsearchContainer esContainer) {
        try {
            final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials("elastic", System.getenv().getOrDefault("ESPSWD", "changeme")));

            final RestClient restClient = RestClient.builder(new HttpHost(esContainer.getHost(), esContainer.getMappedPort(9200), "https"))
                .setHttpClientConfigCallback(hcb -> hcb.setDefaultCredentialsProvider(credentialsProvider))
                .setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                        .setSSLContext(esContainer.createSslContextFromCa())
                )
                .build();
            final RestClientTransport transport = new RestClientTransport(restClient, JSONP_MAPPER);

            final ElasticsearchClient client = new ElasticsearchClient(transport);

            String query = """
                FROM employees
                | WHERE last_name == "Doe"
                | KEEP first_name
                | LIMIT 5
                """;

            ResultSet resultSet = client.esql().query(ResultSetEsqlAdapter.INSTANCE, query);
            Assertions.assertTrue(resultSet.next());
            Assertions.assertEquals("John", resultSet.getString(1));
            Assertions.assertFalse(resultSet.next());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <img src="https://media.tenor.com/qMQ0nbfy6doAAAAC/theshining-killer.gif" alt="here's Johnny!"/>
     */
    protected void checkJohnnyIsHere(JdbcDatabaseContainer<?> dbContainer) {
        try {
            Connection connection = dbContainer.createConnection("");

            ResultSet resultSet = connection.createStatement()
                .executeQuery("select first_name from employees where last_name = 'Doe'");
            Assertions.assertTrue(resultSet.next());
            Assertions.assertEquals("John", resultSet.getString(1));
            Assertions.assertFalse(resultSet.next());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}

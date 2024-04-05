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

    protected static MySQLContainer<?> mySQL  = new MySQLContainer<>(MYSQL_IMAGE);

    protected static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(ELASTICSEARCH_IMAGE);

    protected JacksonJsonpMapper JSONP_MAPPER = new JacksonJsonpMapper();

    static {
        Startables.deepStart(mySQL, elasticsearch).join();
    }

    @BeforeEach
    void prepareContainers() throws InterruptedException {
        DbContainerHelper.runSqlCommand(mySQL,
            "DROP DATABASE %s; CREATE DATABASE %s;".formatted(mySQL.getDatabaseName(), mySQL.getDatabaseName()));
        ElasticsearchContainerHelper.deleteIndices(elasticsearch, "employees");
        DbContainerHelper.runLiquibaseMigrations(mySQL, "config/liquibase/db.changelog-root.xml");
        ElasticsearchContainerHelper.prepareData(elasticsearch, "/config/elasticsearch/");
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

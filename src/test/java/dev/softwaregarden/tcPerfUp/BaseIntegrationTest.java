/*
 *  Copyright (C) 2023 Piotr Przybył
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

import dev.softwaregarden.tcPerfUp.misc.DbContainerHelper;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.MountableFile;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public abstract class BaseIntegrationTest {


    protected static MySQLContainer<?> newDB;

    protected static MySQLContainer<?> oldDB;

    static {
        if (Files.fileNamesIn("src/test/resources/sql/", false).isEmpty()) {
            newDB = new MySQLContainer<>("mysql:8.1")
                .withTmpFs(Map.of("/var/lib/mysql", "rw"));
            oldDB = new MySQLContainer<>("mysql:5.7")
                .withTmpFs(Map.of("/var/lib/mysql", "rw"));

            Startables.deepStart(newDB, oldDB).join();

            DbContainerHelper.runLiquibaseMigrations(newDB, "config/liquibase/db.changelog-root.xml");
            DbContainerHelper.runLiquibaseMigrations(oldDB, "config/liquibase/db.changelog-root.xml");

            DbContainerHelper.snapshotMySQL(
                newDB,
                "src/test/resources/sql/schemaNew.sql",
                "/tmp/schema.sql");
            DbContainerHelper.snapshotMySQL(
                oldDB,
                "src/test/resources/sql/schemaOld.sql",
                "/tmp/schema.sql");
        } else {
            newDB = new MySQLContainer<>("mysql:8.1")
                .withCopyFileToContainer(
                    MountableFile.forHostPath("src/test/resources/sql/schemaNew.sql"), "/tmp/schema.sql")
                .withTmpFs(Map.of("/var/lib/mysql", "rw"));

            oldDB = new MySQLContainer<>("mysql:5.7")
                .withCopyFileToContainer(
                    MountableFile.forHostPath("src/test/resources/sql/schemaOld.sql"), "/tmp/schema.sql")
                .withTmpFs(Map.of("/var/lib/mysql", "rw"));

            Startables.deepStart(newDB, oldDB).join();
        }

    }

    @BeforeEach
    void reset() {
        DbContainerHelper.runSql(newDB, "/tmp/schema.sql");
        DbContainerHelper.runSql(oldDB, "/tmp/schema.sql");
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
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

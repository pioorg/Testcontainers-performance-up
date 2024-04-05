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

package dev.softwaregarden.tcPerfUp.misc;


import liquibase.command.CommandScope;
import liquibase.command.CommonArgumentNames;
import liquibase.command.core.UpdateCommandStep;
import liquibase.exception.CommandExecutionException;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;

import java.io.IOException;

public interface DbContainerHelper {

    static void runLiquibaseMigrations(JdbcDatabaseContainer<?> dbContainer, String changelog) {
        try {
            new CommandScope(UpdateCommandStep.COMMAND_NAME[0])
                .addArgumentValue(CommonArgumentNames.CHANGELOG_FILE.getArgumentName(), changelog)
                .addArgumentValue(CommonArgumentNames.URL.getArgumentName(), dbContainer.getJdbcUrl())
                .addArgumentValue(CommonArgumentNames.USERNAME.getArgumentName(), dbContainer.getUsername())
                .addArgumentValue(CommonArgumentNames.PASSWORD.getArgumentName(), dbContainer.getPassword())
                .execute();
        } catch (CommandExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    static void runSqlCommand(MySQLContainer<?> mySQLContainer, String command) {
        Container.ExecResult result;
        try {
            result = mySQLContainer.execInContainer(
                "mysql",
                "-u", mySQLContainer.getUsername(),
                "-p" + mySQLContainer.getPassword(),
                "-D", mySQLContainer.getDatabaseName(),
                "-e", command);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (result.getExitCode() != 0) {
            throw new RuntimeException("Cannot run the script: [%s] [%s]".formatted(result.getStdout(), result.getStderr()));
        }
    }

    static void snapshotMySQL(MySQLContainer<?> mySQLContainer, String pathInContainer) {
        Container.ExecResult result = null;
        try {
            result = mySQLContainer.execInContainer(
                "mysqldump",
                "-u",
                mySQLContainer.getUsername(),
                "-p" + mySQLContainer.getPassword(),
                "-r",
                pathInContainer,
                "--add-drop-database",
                "--compact",
//                "--no-data",
                "--databases",
                mySQLContainer.getDatabaseName());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (result.getExitCode() != 0) {
            throw new RuntimeException("Cannot prepare the DB dump");
        }
    }

    static void runScript(MySQLContainer<?> mySQLContainer, String scriptPath) {
        Container.ExecResult result;
        try {
            result = mySQLContainer.execInContainer(
                "/bin/sh",
                "-c",
                "mysql -u %s -p%s -D %s < %s"
                    .formatted(
                        mySQLContainer.getUsername(),
                        mySQLContainer.getPassword(),
                        mySQLContainer.getDatabaseName(),
                        scriptPath
                    ));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (result.getExitCode() != 0) {
            throw new RuntimeException("Cannot run the script: [%s] [%s]".formatted(result.getStdout(), result.getStderr()));
        }
    }
}

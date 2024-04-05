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
import org.testcontainers.containers.JdbcDatabaseContainer;

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
}

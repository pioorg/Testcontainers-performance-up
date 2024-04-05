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
package dev.softwaregarden.tcPerfUp.misc;

import org.testcontainers.containers.Container;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface ElasticsearchContainerHelper {

    Logger logger = Logger.getLogger(ElasticsearchContainerHelper.class.getName());

    static void prepareData(ElasticsearchContainer elasticsearch, String dir) {

        logger.log(Level.INFO, "Running Elasticsearch migrations");
        List<EsCurlCall> toCall = List.of(
//            new EsCall("GET", "", null),
            new EsCurlCall("PUT", "/employees", loadResource(dir + "mapping.json")),
            new EsCurlCall("POST", "/employees/_bulk?refresh=true", loadResource(dir + "employees.njson")),
            new EsCurlCall("PUT", "/employees/_mapping", loadResource(dir + "mapping_update.json")),
            new EsCurlCall("POST", "/employees/_bulk?refresh=true", loadResource(dir + "employees_update.njson"))
        );

        for (EsCurlCall esCurlCall : toCall) {
            esCurlCall.makeCurlCall(elasticsearch);
        }
        logger.log(Level.INFO, "Finished Elasticsearch migrations");
    }


    private static String loadResource(String resource) {
        try (InputStream stream = ElasticsearchContainerHelper.class.getResourceAsStream(resource)) {
            return new String(stream.readAllBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void deleteIndices(ElasticsearchContainer elasticsearch, String... indices) {
        for (String index : indices) {
            new EsCurlCall("DELETE", "/" + index, null).makeCurlCall(elasticsearch);
        }
    }

    record EsCurlCall(String method, String endpoint, String payload) {
        private void makeCurlCall(ElasticsearchContainer elasticsearch) {
            List<String> call = new ArrayList<>(List.of(
                "/usr/bin/curl", "-k", "--silent", "-u", "elastic:changeme", "-H", "Content-Type: application/json",
                "-X", this.method, "https://localhost:9200" + this.endpoint));
            if (this.payload != null && !this.payload.isEmpty()) {
                call.add("-d");
                call.add(this.payload);
            }

            Container.ExecResult execResult = null;
            try {
                execResult = elasticsearch.execInContainer(call.toArray(new String[0]));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if (execResult.getExitCode() != 0) {
                throw new RuntimeException("Error when calling %s: [%s] [%s]".formatted(this, execResult.getStdout(), execResult.getStderr()));
            }
        }
    }
}

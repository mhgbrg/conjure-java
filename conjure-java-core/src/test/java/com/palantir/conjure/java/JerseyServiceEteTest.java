/*
 * (c) Copyright 2018 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.conjure.java;

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.conjure.java.lib.SafeLong;
import com.palantir.conjure.java.services.JerseyServiceGenerator;
import com.palantir.conjure.spec.ConjureDefinition;
import com.palantir.product.EmptyPathService;
import com.palantir.product.EteService;
import com.palantir.remoting3.ext.jackson.ObjectMappers;
import com.palantir.remoting3.jaxrs.JaxRsClient;
import com.palantir.ri.ResourceIdentifier;
import com.palantir.tokens.auth.AuthHeader;
import io.dropwizard.Configuration;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class JerseyServiceEteTest extends TestBase {

    @ClassRule
    public static final TemporaryFolder folder = new TemporaryFolder();

    @ClassRule
    public static final DropwizardAppRule<Configuration> RULE =
            new DropwizardAppRule<>(EteTestServer.class);

    private final EteService client;

    public JerseyServiceEteTest() {
        client = JaxRsClient.create(
                EteService.class,
                EteTestServer.clientUserAgent(),
                EteTestServer.clientConfiguration());
    }

    @Test
    public void jaxrs_client_can_make_a_call_to_an_empty_path() throws Exception {
        EmptyPathService emptyPathClient = JaxRsClient.create(
                EmptyPathService.class,
                EteTestServer.clientUserAgent(),
                EteTestServer.clientConfiguration());
        assertThat(emptyPathClient.emptyPath()).isEqualTo(true);
    }

    @Ignore // string returns in Jersey should use a mandated wrapper alias type
    @Test
    public void http_remoting_client_can_retrieve_a_string_from_witchcraft() throws Exception {
        assertThat(client.string(AuthHeader.valueOf("authHeader")))
                .isEqualTo("Hello, world!");
    }

    @Test
    public void http_remoting_client_can_retrieve_a_double_from_witchcraft() throws Exception {
        assertThat(client.double_(AuthHeader.valueOf("authHeader")))
                .isEqualTo(1 / 3d);
    }

    @Test
    public void http_remoting_client_can_retrieve_a_boolean_from_witchcraft() throws Exception {
        assertThat(client.boolean_(AuthHeader.valueOf("authHeader")))
                .isEqualTo(true);
    }

    @Test
    public void http_remoting_client_can_retrieve_a_safelong_from_witchcraft() throws Exception {
        assertThat(client.safelong(AuthHeader.valueOf("authHeader")))
                .isEqualTo(SafeLong.of(12345));
    }

    @Test
    public void http_remoting_client_can_retrieve_an_rid_from_witchcraft() throws Exception {
        assertThat(client.rid(AuthHeader.valueOf("authHeader")))
                .isEqualTo(ResourceIdentifier.of("ri.foundry.main.dataset.1234"));
    }

    @Ignore // string returns in Jersey should use a mandated wrapper alias type
    @Test
    public void http_remoting_client_can_retrieve_an_optional_string_from_witchcraft() throws Exception {
        assertThat(client.optionalString(AuthHeader.valueOf("authHeader")))
                .isEqualTo(Optional.of("foo"));
    }

    @Ignore // Dropwizard returns 404 for empty optional
    @Test
    public void jaxrs_client_can_retrieve_an_optional_empty_from_witchcraft() throws Exception {
        assertThat(client.optionalEmpty(AuthHeader.valueOf("authHeader")))
                .isEqualTo(Optional.empty());
    }

    @Test
    public void http_remoting_client_can_retrieve_a_date_time_from_witchcraft() throws Exception {
        assertThat(client.datetime(AuthHeader.valueOf("authHeader")))
                .isEqualTo(ZonedDateTime.ofInstant(Instant.ofEpochMilli(1234), ZoneId.from(ZoneOffset.UTC)));
    }

    @BeforeClass
    public static void beforeClass() throws IOException {
        // ConjureDefinition def = Conjure.parse(
        //         ImmutableList.of(new File("src/test/resources/ete-service.yml")));
        // ObjectMappers.newServerObjectMapper().writeValue(new File("src/test/resources/ete-service.json"), def);
        ConjureDefinition def = ObjectMappers.newServerObjectMapper().readValue(
                new File("src/test/resources/ete-service.json"), ConjureDefinition.class);
        List<Path> files = new JerseyServiceGenerator().emit(def, folder.getRoot());

        for (Path file : files) {
            Path output = Paths.get("src/integrationInput/java/com/palantir/product/" + file.getFileName());
            if (Boolean.valueOf(System.getProperty("recreate", "false"))) {
                Files.deleteIfExists(output);
                Files.copy(file, output);
            }

            assertThat(readFromFile(file)).isEqualTo(readFromFile(output));
        }
    }
}
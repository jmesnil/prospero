/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package integration;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.wildfly.prospero.model.ChannelRef;
import org.wildfly.prospero.api.InstallationMetadata;
import org.wildfly.prospero.model.ProsperoConfig;
import org.wildfly.prospero.model.RepositoryRef;

public class TestUtil {

    public static final Path MANIFEST_FILE_PATH =
            Paths.get(InstallationMetadata.METADATA_DIR, InstallationMetadata.MANIFEST_FILE_NAME);
    public static final Path PROVISION_CONFIG_FILE_PATH =
            Paths.get(InstallationMetadata.METADATA_DIR, InstallationMetadata.PROSPERO_CONFIG_FILE_NAME);

    public static URL prepareProvisionConfigAsUrl(String channelDescriptor) throws IOException {
        final Path provisionConfigFile = Files.createTempFile("channels", "yaml");
        provisionConfigFile.toFile().deleteOnExit();

        final Path path = prepareProvisionConfigAsUrl(provisionConfigFile, channelDescriptor);
        return path.toUri().toURL();
    }

    public static Path prepareProvisionConfig(String channelDescriptor) throws IOException {
        final Path provisionConfigFile = Files.createTempFile("channels", "yaml");
        provisionConfigFile.toFile().deleteOnExit();

        return prepareProvisionConfigAsUrl(provisionConfigFile, channelDescriptor);
    }

    public static Path prepareProvisionConfigAsUrl(Path provisionConfigFile, String... channelDescriptor)
            throws IOException {
        List<URL> channelUrls = Arrays.stream(channelDescriptor)
                .map(d->TestUtil.class.getClassLoader().getResource(d))
                .collect(Collectors.toList());
        List<ChannelRef> channels = new ArrayList<>();
        List<RepositoryRef> repositories = WfCoreTestBase.defaultRemoteRepositories().stream()
                .map(r->new RepositoryRef(r.getId(), r.getUrl())).collect(Collectors.toList());
        for (int i=0; i<channelUrls.size(); i++) {
            channels.add(new ChannelRef(null, channelUrls.get(i).toString()));
        }
        new ProsperoConfig(channels, repositories).writeConfig(provisionConfigFile.toFile());

        return provisionConfigFile;
    }
}

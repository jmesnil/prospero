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

package org.wildfly.prospero.api;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.aether.repository.RemoteRepository;
import org.jboss.galleon.ProvisioningException;
import org.wildfly.prospero.Messages;
import org.wildfly.prospero.api.exceptions.ArtifactResolutionException;
import org.wildfly.prospero.model.ChannelRef;
import org.wildfly.prospero.model.ProsperoConfig;
import org.wildfly.prospero.model.RepositoryRef;

public class ProvisioningDefinition {

    private static final String REPO_TYPE = "default";

    private final String fpl;
    private final List<ChannelRef> channels = new ArrayList<>();
    private final Set<String> includedPackages = new HashSet<>();
    private final List<RemoteRepository> repositories = new ArrayList<>();
    private final Path definition;

    private ProvisioningDefinition(Builder builder) throws ArtifactResolutionException {
        final Optional<String> fpl = Optional.ofNullable(builder.fpl);
        final Optional<Path> definition = Optional.ofNullable(builder.definitionFile);
        final Optional<String> channelRepo = Optional.ofNullable(builder.channelRepo);
        final Optional<Path> provisionConfigFile = Optional.ofNullable(builder.provisionConfigFile);
        final Optional<URL> channel = Optional.ofNullable(builder.channel);
        final Optional<Set<String>> includedPackages = Optional.ofNullable(builder.includedPackages);

        if (builder.repositories != null) {
            this.repositories.addAll(builder.repositories);
        }

        this.includedPackages.addAll(includedPackages.orElse(Collections.emptySet()));

        try {
            if (fpl.isPresent() && WellKnownFeaturePacks.isWellKnownName(fpl.get())) {
                WellKnownFeaturePacks featurePackInfo = WellKnownFeaturePacks.getByName(fpl.get());
                this.fpl = featurePackInfo.location;
                this.definition = null;
                this.includedPackages.addAll(featurePackInfo.packages);
                this.repositories.addAll(featurePackInfo.repositories);
                setUpBuildEnv(channelRepo, provisionConfigFile, channel, featurePackInfo.channelGav);
            } else if (provisionConfigFile.isPresent()) {
                this.fpl = fpl.orElse(null);
                this.definition = definition.orElse(null);
                final ProsperoConfig record = ProsperoConfig.readConfig(provisionConfigFile.get());
                if (record.getChannels() != null) {
                    this.channels.addAll(record.getChannels());
                }
                this.repositories.clear();
                this.repositories.addAll(record.getRepositories().stream().map(RepositoryRef::toRemoteRepository).collect(Collectors.toList()));
            } else {
                // TODO: provisionConfigFile needn't be mandatory, we could still collect all required data from the
                //  other options (channel, channelRepo - perhaps both should be made collections)
                throw new IllegalArgumentException(
                        String.format("Incomplete configuration: either a predefined fpl (%s) or a provisionConfigFile must be given.",
                                String.join(", ", WellKnownFeaturePacks.getNames())));
            }
        } catch (IOException e) {
            throw new ArtifactResolutionException("Unable to resolve channel definition: " + e.getMessage(), e);
        }

        if (channels.isEmpty()) {
            throw Messages.MESSAGES.noChannelReference();
        }
    }

    private void setUpBuildEnv(Optional<String> channelRepo, Optional<Path> provisionConfigFile, Optional<URL> channel, String channelGA) throws IOException {
        if (!provisionConfigFile.isPresent() && !channel.isPresent()) {
            this.channels.add(new ChannelRef(channelGA, null));
            if (channelRepo.isPresent()) {
                String[] urls = channelRepo.get().split(",");
                for (int i = 0; i < urls.length; i++) {
                    String channelRepoId = "channel-" + (i + 1);
                    this.repositories.add(
                            new RemoteRepository.Builder(channelRepoId, REPO_TYPE, channelRepo.get()).build());
                }
            }
        } else if (channel.isPresent()) {
            this.channels.add(new ChannelRef(null, channel.get().toString()));
        } else {
            final ProsperoConfig record = ProsperoConfig.readConfig(provisionConfigFile.get());
            this.channels.addAll(record.getChannels());
            this.repositories.clear();
            this.repositories.addAll(record.getRepositories().stream().map(RepositoryRef::toRemoteRepository).collect(Collectors.toList()));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public Set<String> getIncludedPackages() {
        return includedPackages;
    }

    public String getFpl() {
        return fpl;
    }

    public List<ChannelRef> getChannelRefs() {
        return channels;
    }

    public List<RemoteRepository> getRepositories() {
        return repositories;
    }

    public Path getDefinition() {
        return definition;
    }

    public ProsperoConfig getProsperoConfig() {
        return new ProsperoConfig(channels, repositories.stream().map(RepositoryRef::new).collect(Collectors.toList()));
    }

    public static class Builder {
        private String fpl;
        private Path provisionConfigFile;
        private Path definitionFile;
        private String channelRepo;
        private Set<String> includedPackages;
        private URL channel;
        private List<RemoteRepository> repositories;

        public ProvisioningDefinition build() throws ArtifactResolutionException {
            return new ProvisioningDefinition(this);
        }

        public Builder setFpl(String fpl) {
            this.fpl = fpl;
            return this;
        }

        public Builder setProvisionConfig(Path provisionConfigFile) {
            this.provisionConfigFile = provisionConfigFile;
            return this;
        }

        public Builder setChannelRepo(String channelRepo) {
            this.channelRepo = channelRepo;
            return this;
        }

        public Builder setIncludedPackages(Set<String> includedPackages) {
            this.includedPackages = includedPackages;
            return this;
        }

        public Builder setChannel(String channel) throws ProvisioningException {
            if (channel != null) {
                try {
                    this.channel = new URL(channel);
                } catch (MalformedURLException e) {
                    try {
                        this.channel = Paths.get(channel).toAbsolutePath().toUri().toURL();
                    } catch (MalformedURLException ex) {
                        throw new ProvisioningException("Unrecognized path to channels file", ex);
                    }
                }
            }
            return this;
        }

        public Builder setRepositories(List<RemoteRepository> repositories) {
            this.repositories = repositories;
            return this;
        }

        public Builder setDefinitionFile(Path provisionDefinition) {
            this.definitionFile = provisionDefinition;
            return this;
        }
    }
}

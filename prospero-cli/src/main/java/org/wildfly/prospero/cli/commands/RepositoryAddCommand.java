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

package org.wildfly.prospero.cli.commands;

import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

import org.wildfly.prospero.actions.Console;
import org.wildfly.prospero.actions.MetadataAction;
import org.wildfly.prospero.cli.ActionFactory;
import org.wildfly.prospero.cli.CliMessages;
import org.wildfly.prospero.cli.ReturnCodes;
import picocli.CommandLine;

@CommandLine.Command(name = CliConstants.ADD)
public class RepositoryAddCommand extends AbstractCommand {

    @CommandLine.Parameters(index = "0", paramLabel = CliConstants.REPO_ID, descriptionKey = "repoId")
    String repoId;

    @CommandLine.Parameters(index = "1", paramLabel = CliConstants.REPO_URL, descriptionKey = "repoUrl")
    URL url;

    @CommandLine.Option(names = CliConstants.DIR)
    Optional<Path> directory;

    public RepositoryAddCommand(Console console, ActionFactory actionFactory) {
        super(console, actionFactory);
    }

    @Override
    public Integer call() throws Exception {
        Path installationDirectory = determineInstallationDirectory(directory);
        MetadataAction metadataAction = actionFactory.metadataActions(installationDirectory);
        metadataAction.addRepository(repoId, url);
        console.println(CliMessages.MESSAGES.repositoryAdded(repoId));
        return ReturnCodes.SUCCESS;
    }
}

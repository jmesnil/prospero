/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.redhat.prospero.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.redhat.prospero.api.ChannelRef;
import com.redhat.prospero.api.MetadataException;
import com.redhat.prospero.api.Server;
import org.jboss.galleon.ProvisioningException;

class InstallArgs {
   private final CliMain.ActionFactory actionFactory;

   InstallArgs(CliMain.ActionFactory actionFactory) {
      this.actionFactory = actionFactory;
   }

   void handleArgs(Map<String, String> parsedArgs) throws ArgumentParsingException {
      String dir = parsedArgs.get(CliMain.TARGET_PATH_ARG);
      String fpl = parsedArgs.get(CliMain.FPL_ARG);
      String channelFile = parsedArgs.get(CliMain.CHANNEL_FILE_ARG);
      String channelRepo = parsedArgs.get(CliMain.CHANNEL_REPO);
      Map<String, String> channelUrls = new HashMap<>();

      if (dir == null || dir.isEmpty()) {
         throw new ArgumentParsingException("Target dir argument (--%s) need to be set on install command", CliMain.TARGET_PATH_ARG);
      }
      if (fpl == null || fpl.isEmpty()) {
         throw new ArgumentParsingException("Feature pack name argument (--%s) need to be set on install command", CliMain.FPL_ARG);
      }


      if (!fpl.equals("eap") && !fpl.equals("wildfly") && (channelFile == null || channelFile.isEmpty())) {
         throw new ArgumentParsingException("Channel file argument (--%s) need to be set when using custom fpl", CliMain.CHANNEL_FILE_ARG);
      }

      try {
         final Path installationDir = Paths.get(dir).toAbsolutePath();
         final Server server = new Server(fpl, channelFile==null?null:Paths.get(channelFile).toAbsolutePath(), Optional.ofNullable(channelRepo));
         final List<ChannelRef> channels = server.getChannelRefs();
         fpl = server.getFpl();

         actionFactory.install(installationDir).provision(fpl, channels);
      } catch (IOException e) {
         e.printStackTrace();
      } catch (ProvisioningException | MetadataException e) {
         e.printStackTrace();
      }
   }
}
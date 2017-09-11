/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.internal.protocol.security.server;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.geode.GemFireConfigException;
import org.apache.geode.internal.cache.tier.sockets.ServiceLoadingFailureException;

public class AuthenticatorLookupService {
  private Map<String, Class<? extends Authenticator>> authenticators = null;

  public AuthenticatorLookupService() {
    if (authenticators == null) {
      initializeAuthenticatorsMap();
    }
  }

  private synchronized void initializeAuthenticatorsMap() {
    if (authenticators != null) {
      return;
    }
    authenticators = new HashMap<>();
    ServiceLoader<Authenticator> loader = ServiceLoader.load(Authenticator.class);
    for (Authenticator streamAuthenticator : loader) {
      authenticators.put(streamAuthenticator.implementationID(), streamAuthenticator.getClass());
    }
  }

  public Authenticator getAuthenticator() {
    String authenticationMode = System.getProperty("geode.protocol-authentication-mode", "NOOP");

    Class<? extends Authenticator> streamAuthenticatorClass =
        authenticators.get(authenticationMode);
    if (streamAuthenticatorClass == null) {
      throw new GemFireConfigException(
          "Could not find implementation for Authenticator with implementation ID "
              + authenticationMode);
    } else {
      try {
        return streamAuthenticatorClass.newInstance();
      } catch (InstantiationException | IllegalAccessException e) {
        throw new ServiceLoadingFailureException(
            "Unable to instantiate authenticator for ID " + authenticationMode, e);
      }
    }
  }
}

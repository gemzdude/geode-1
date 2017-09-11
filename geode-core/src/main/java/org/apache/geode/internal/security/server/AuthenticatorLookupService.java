package org.apache.geode.internal.security.server;

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

package org.apache.geode.internal.protocol;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.apache.geode.internal.cache.tier.sockets.ServiceLoadingFailureException;

public class ProtocolMessageHandlerLookupService {
  private ClientProtocolMessageHandler protocolHandler;

  public ProtocolMessageHandlerLookupService() {
    protocolHandler = findFirstProtocolMessageHandler();
  }

  private ClientProtocolMessageHandler findFirstProtocolMessageHandler() {
    ServiceLoader<ClientProtocolMessageHandler> loader =
        ServiceLoader.load(ClientProtocolMessageHandler.class);
    Iterator<ClientProtocolMessageHandler> iterator = loader.iterator();

    if (!iterator.hasNext()) {
      throw new ServiceLoadingFailureException(
          "There is no ClientProtocolMessageHandler implementation found in JVM");
    }

    return iterator.next();
  }

  public ClientProtocolMessageHandler lookupProtocolHandler(String protocolType) {
    // TODO Do we need to make provision for different protocols here right now?
    return protocolHandler;
  }
}

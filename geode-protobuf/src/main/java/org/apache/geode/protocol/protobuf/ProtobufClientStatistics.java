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
package org.apache.geode.protocol.protobuf;

import org.apache.geode.StatisticDescriptor;
import org.apache.geode.Statistics;
import org.apache.geode.StatisticsFactory;
import org.apache.geode.StatisticsType;
import org.apache.geode.internal.cache.tier.sockets.ClientProtocolStatistics;

public class ProtobufClientStatistics implements ClientProtocolStatistics {
  private final StatisticsType statType;
  private final Statistics stats;
  private final int currentClientConnectionsId;
  private final int clientConnectionTerminationsId;
  private final int clientConnectionStartsId;
  private final int bytesReceivedId;
  private final int bytesSentId;

  public ProtobufClientStatistics(StatisticsFactory statisticsFactory, String statisticsName, String typeName) {
    StatisticDescriptor[] serverStatDescriptors = new StatisticDescriptor[]{
        statisticsFactory.createIntGauge("currentClientConnections",
            "Number of sockets accepted and used for client to server messaging.", "sockets"),
        statisticsFactory.createIntCounter("clientConnectionStarts",
            "Number of sockets accepted and used for client to server messaging.", "sockets"),
        statisticsFactory.createIntCounter("clientConnectionTerminations",
            "Number of sockets that were used for client to server messaging.", "sockets"),
        statisticsFactory.createLongCounter("bytesReceived",
            "Bytes received from client messaging.", "bytes"),
        statisticsFactory.createLongCounter("bytesSent",
            "Bytes sent for client messaging.", "bytes")
    };
    statType = statisticsFactory.createType(typeName, "Protobuf client/server statistics", serverStatDescriptors);
    this.stats = statisticsFactory.createAtomicStatistics(statType, statisticsName);
    currentClientConnectionsId = this.stats.nameToId("currentClientConnections");
    clientConnectionStartsId = this.stats.nameToId("clientConnectionStarts");
    clientConnectionTerminationsId = this.stats.nameToId("clientConnectionTerminations");
    bytesReceivedId = this.stats.nameToId("bytesReceived");
    bytesSentId = this.stats.nameToId("bytesSent");
  }

  @Override
  public void clientConnected() {
    stats.incInt(currentClientConnectionsId, 1);
    stats.incInt(clientConnectionStartsId, 1);
  }

  @Override
  public void clientDisconnected() {
    stats.incInt(currentClientConnectionsId, -1);
    stats.incInt(clientConnectionTerminationsId, 1);
  }

  public void messageReceived(int bytes) {
    stats.incLong(bytesReceivedId, bytes);
  }

  public void messageSent(int bytes) {
    stats.incLong(bytesSentId, bytes);
  }
}

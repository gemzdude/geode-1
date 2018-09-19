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
package org.apache.geode.management.internal.cli.commands;

import static org.apache.geode.management.internal.cli.i18n.CliStrings.LIST_MEMBER;
import static org.apache.geode.management.internal.cli.i18n.CliStrings.STATUS_LOCATOR;
import static org.apache.geode.management.internal.cli.result.InfoResultData.RESULT_CONTENT_MESSAGE;
import static org.apache.geode.management.internal.cli.result.ResultData.RESULT_CONTENT;
import static org.apache.geode.management.internal.cli.shell.Gfsh.ENV_APP_QUIET_EXECUTION;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.springframework.shell.ShellException;
import org.springframework.shell.core.CommandResult;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.configuration.CacheConfig;
import org.apache.geode.cache.configuration.JndiBindingsType;
import org.apache.geode.distributed.DistributedMember;
import org.apache.geode.internal.logging.LogService;
import org.apache.geode.management.cli.CliMetaData;
import org.apache.geode.management.cli.SingleGfshCommand;
import org.apache.geode.management.internal.cli.functions.CliFunctionResult;
import org.apache.geode.management.internal.cli.functions.CreateJndiBindingFunction;
import org.apache.geode.management.internal.cli.i18n.CliStrings;
import org.apache.geode.management.internal.cli.result.InfoResultData;
import org.apache.geode.management.internal.cli.result.LegacyCommandResult;
import org.apache.geode.management.internal.cli.result.ModelCommandResult;
import org.apache.geode.management.internal.cli.result.model.ResultModel;
import org.apache.geode.management.internal.cli.shell.Gfsh;
import org.apache.geode.management.internal.security.ResourceOperation;
import org.apache.geode.security.ResourcePermission;

public class CreateJndiBindingCommand extends SingleGfshCommand {
  private static final Logger logger = LogService.getLogger();

  static final String CREATE_JNDIBINDING = "create jndi-binding";
  static final String CREATE_JNDIBINDING__HELP =
      "Create a jndi binding that holds the configuration for the XA datasource.";
  static final String BLOCKING_TIMEOUT_SECONDS = "blocking-timeout-seconds";
  static final String BLOCKING_TIMEOUT_SECONDS__HELP =
      "This element specifies the maximum time to block while waiting for a connection before throwing an exception.";
  static final String CONNECTION_POOLED_DATASOURCE_CLASS = "conn-pooled-datasource-class";
  static final String CONNECTION_POOLED_DATASOURCE_CLASS__HELP =
      "This is the fully qualified name of the connection pool implementation to hold XA datasource connections.";
  static final String CONNECTION_URL = "connection-url";
  static final String CONNECTION_URL__HELP =
      "This is the JDBC driver connection URL string, for example, jdbc:hsqldb:hsql://localhost:1701.";
  static final String IDLE_TIMEOUT_SECONDS = "idle-timeout-seconds";
  static final String IDLE_TIMEOUT_SECONDS__HELP =
      "This element specifies the time a connection may be idle before being closed.";
  static final String INIT_POOL_SIZE = "init-pool-size";
  static final String INIT_POOL_SIZE__HELP =
      "This element specifies the initial number of connections the pool should hold.";
  static final String JDBC_DRIVER_CLASS = "jdbc-driver-class";
  static final String JDBC_DRIVER_CLASS__HELP =
      "This is the fully qualified name of the JDBC driver class.";
  static final String JNDI_NAME = "name";
  static final String JNDI_NAME__HELP = "Name of the binding to be created.";
  static final String LOGIN_TIMEOUT_SECONDS = "login-timeout-seconds";
  static final String LOGIN_TIMEOUT_SECONDS__HELP =
      "Time in seconds after which the client thread for retrieving connection will experience timeout.";
  static final String MANAGED_CONN_FACTORY_CLASS = "managed-conn-factory-class";
  static final String MANAGED_CONN_FACTORY_CLASS__HELP =
      "This is the fully qualified name of the connection factory implementation.";
  static final String MAX_POOL_SIZE = "max-pool-size";
  static final String MAX_POOL_SIZE__HELP =
      "This element specifies the maximum number of connections for a pool. No more than the max-pool-size number of connections will be created in a pool.";
  static final String PASSWORD = "password";
  static final String PASSWORD__HELP =
      "This element specifies the default password used when creating a new connection.";
  static final String TRANSACTION_TYPE = "transaction-type";
  static final String TRANSACTION_TYPE__HELP = "Type of the transaction.";
  static final String TYPE = "type";
  static final String TYPE__HELP =
      "Type of the XA datasource. The following types are pre-defined by the product: MANAGED, SIMPLE, POOLED, XAPOOLED.";
  static final String USERNAME = "username";
  static final String USERNAME__HELP =
      "This element specifies the default username used when creating a new connection.";
  static final String XA_DATASOURCE_CLASS = "xa-datasource-class";
  static final String XA_DATASOURCE_CLASS__HELP =
      "The fully qualified name of the javax.sql.XADataSource implementation class.";
  static final String IFNOTEXISTS__HELP =
      "Skip the create operation when a jndi binding with the same name already exists.  Without specifying this option, this command execution results into an error.";
  static final String DATASOURCE_CONFIG_PROPERTIES = "datasource-config-properties";
  static final String DATASOURCE_CONFIG_PROPERTIES_HELP =
      "Properties for the custom XADataSource driver. Append json string containing (name, type, value) to set any property. Eg: --datasource-config-properties={'name':'name1','type':'type1','value':'value1'},{'name':'name2','type':'type2','value':'value2'}";

  @CliCommand(value = CREATE_JNDIBINDING, help = CREATE_JNDIBINDING__HELP)
  @CliMetaData(relatedTopic = CliStrings.TOPIC_GEODE_REGION, requireLocalExecution = true)
  @ResourceOperation(resource = ResourcePermission.Resource.CLUSTER,
      operation = ResourcePermission.Operation.MANAGE)
  public ResultModel createJDNIBinding(
      @CliOption(key = BLOCKING_TIMEOUT_SECONDS,
          help = BLOCKING_TIMEOUT_SECONDS__HELP) Integer blockingTimeout,
      @CliOption(key = CONNECTION_POOLED_DATASOURCE_CLASS,
          help = CONNECTION_POOLED_DATASOURCE_CLASS__HELP) String connectionPooledDatasource,
      @CliOption(key = CONNECTION_URL, mandatory = true,
          help = CONNECTION_URL__HELP) String connectionUrl,
      @CliOption(key = IDLE_TIMEOUT_SECONDS, help = IDLE_TIMEOUT_SECONDS__HELP) Integer idleTimeout,
      @CliOption(key = INIT_POOL_SIZE, help = INIT_POOL_SIZE__HELP) Integer initPoolSize,
      @CliOption(key = JDBC_DRIVER_CLASS, mandatory = true,
          help = JDBC_DRIVER_CLASS__HELP) String jdbcDriver,
      @CliOption(key = JNDI_NAME, mandatory = true, help = JNDI_NAME__HELP) String jndiName,
      @CliOption(key = LOGIN_TIMEOUT_SECONDS,
          help = LOGIN_TIMEOUT_SECONDS__HELP) Integer loginTimeout,
      @CliOption(key = MANAGED_CONN_FACTORY_CLASS,
          help = MANAGED_CONN_FACTORY_CLASS__HELP) String managedConnFactory,
      @CliOption(key = MAX_POOL_SIZE, help = MAX_POOL_SIZE__HELP) Integer maxPoolSize,
      @CliOption(key = PASSWORD, help = PASSWORD__HELP) String password,
      @CliOption(key = TRANSACTION_TYPE, help = TRANSACTION_TYPE__HELP) String transactionType,
      @CliOption(key = TYPE, mandatory = true, help = TYPE__HELP) DATASOURCE_TYPE type,
      @CliOption(key = USERNAME, help = USERNAME__HELP) String username,
      @CliOption(key = XA_DATASOURCE_CLASS, help = XA_DATASOURCE_CLASS__HELP) String xaDataSource,
      @CliOption(key = CliStrings.IFNOTEXISTS, help = IFNOTEXISTS__HELP,
          specifiedDefaultValue = "true", unspecifiedDefaultValue = "false") boolean ifNotExists,
      @CliOption(key = DATASOURCE_CONFIG_PROPERTIES, optionContext = "splittingRegex=,(?![^{]*\\})",
          help = DATASOURCE_CONFIG_PROPERTIES_HELP) JndiBindingsType.JndiBinding.ConfigProperty[] dsConfigProperties) {

    JndiBindingsType.JndiBinding configuration = new JndiBindingsType.JndiBinding();
    configuration.setBlockingTimeoutSeconds(Objects.toString(blockingTimeout, null));
    configuration.setConnPooledDatasourceClass(connectionPooledDatasource);
    configuration.setConnectionUrl(connectionUrl);
    configuration.setIdleTimeoutSeconds(Objects.toString(idleTimeout, null));
    configuration.setInitPoolSize(Objects.toString(initPoolSize, null));
    configuration.setJdbcDriverClass(jdbcDriver);
    configuration.setJndiName(jndiName);
    configuration.setLoginTimeoutSeconds(Objects.toString(loginTimeout, null));
    configuration.setManagedConnFactoryClass(managedConnFactory);
    configuration.setMaxPoolSize(Objects.toString(maxPoolSize, null));
    configuration.setPassword(password);
    configuration.setTransactionType(transactionType);
    configuration.setType(type.getType());
    configuration.setUserName(username);
    configuration.setXaDatasourceClass(xaDataSource);

    Gfsh gfsh = Gfsh.getCurrentInstance();

    if (gfsh == null) {
      throw new ShellException("Error when attempting to access local shell");
    }

    if (username == null) {
      username = gfsh.readText("Username: ");
    }

    if (password == null) {
      // gfsh.readPassword("Password");
    }

    if (dsConfigProperties != null && dsConfigProperties.length > 0)
      configuration.getConfigProperties().addAll(Arrays.asList(dsConfigProperties));

    // gfsh.setEnvProperty(ENV_APP_QUIET_EXECUTION, "true");
    CommandResult cmdResult = gfsh.executeCommand(LIST_MEMBER);

    // assertThat(result.getStatus()).isEqualTo(Result.Status.OK);

    Map<String, List<String>> table =
        ((ModelCommandResult) cmdResult.getResult())
            .getMapFromTableContent(ListMembersCommand.MEMBERS_SECTION);

    String locatorName = null;
    List<String> ids = table.get("Id");
    List<String> names = table.get("Name");
    for (int i = 0; i < ids.size() && i < names.size(); i++) {
      if (ids.get(i).contains("[Coordinator]")) {
        locatorName = names.get(i);
        break;
      }
    }
    if (locatorName == null) {
      return ResultModel.createInfo("No coordinator found.");
    }

    cmdResult = gfsh.executeCommand(STATUS_LOCATOR + " --name=" + locatorName);
    gfsh.setEnvProperty(ENV_APP_QUIET_EXECUTION, "false");

    InfoResultData infoResultData =
        (InfoResultData) ((LegacyCommandResult) cmdResult.getResult()).getResultData();
    String locatorStatus =
        ((JSONArray) infoResultData.getGfJsonObject().getJSONObject(RESULT_CONTENT)
            .get(RESULT_CONTENT_MESSAGE)).toString();

    gfsh.printAsInfo(locatorName + ": " + locatorStatus);

    Pattern MY_PATTERN = Pattern.compile(locatorName + " on (.*?)\\[(.*?)\\]");
    // MY_PATTERN = Pattern.compile(username);

    String locatorHost = null;
    String locatorPort = null;
    Matcher m = MY_PATTERN.matcher(locatorStatus);
    while (m.find()) {
      locatorHost = m.group(1);
      locatorPort = m.group(2);
    }
    gfsh.printAsInfo("OUTPUT: " + locatorHost + " OUT2: " + locatorPort);


    // assertThat(table.get("Name").size()).isEqualTo(4);
    // assertThat(table.get("Name")).contains("locator-0", "server-1", "server-2", "server-3");
    // InternalConfigurationPersistenceService service =
    // (InternalConfigurationPersistenceService) getConfigurationPersistenceService();
    //
    // if (service != null) {
    // CacheConfig cacheConfig = service.getCacheConfig("cluster");
    // if (cacheConfig != null) {
    // JndiBindingsType.JndiBinding existing =
    // CacheElement.findElement(cacheConfig.getJndiBindings(), jndiName);
    // if (existing != null) {
    // throw new EntityExistsException(
    // CliStrings.format("Jndi binding with jndi-name \"{0}\" already exists.", jndiName),
    // ifNotExists);
    // }
    // }
    // }

    ClientCache cache = new ClientCacheFactory()
        .addPoolLocator(locatorHost, Integer.parseInt(locatorPort)).create();

    Set<DistributedMember> targetMembers = new HashSet();

    for (InetSocketAddress iSockAddr : cache.getCurrentServers()) {
      for (DistributedMember dist : cache.getDistributedSystem()
          .findDistributedMembers(iSockAddr.getAddress())) {
        targetMembers.add(dist);
      }
    }

    if (targetMembers != null && targetMembers.size() > 0) {
      List<CliFunctionResult> jndiCreationResult = executeAndGetFunctionResult(
          new CreateJndiBindingFunction(), configuration, targetMembers);
      ResultModel result = ResultModel.createMemberStatusResult(jndiCreationResult);
      result.setConfigObject(configuration);
      return result;
    } else {
      return ResultModel.createInfo("No members found.");
    }
  }

  @Override
  public void updateClusterConfig(String group, CacheConfig config, Object element) {
    config.getJndiBindings().add((JndiBindingsType.JndiBinding) element);
  }

  public enum DATASOURCE_TYPE {
    MANAGED("ManagedDataSource"),
    SIMPLE("SimpleDataSource"),
    POOLED("PooledDataSource"),
    XAPOOLED("XAPooledDataSource");

    private final String type;

    DATASOURCE_TYPE(String type) {
      this.type = type;
    }

    public String getType() {
      return this.type;
    }

    public String getName() {
      return name();
    }
  }
}

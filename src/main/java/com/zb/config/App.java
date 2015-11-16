package com.zb.config;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.config.ChainedDynamicProperty;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicContextualProperty;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.StringDerivedProperty;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Config examples
 *
 * Given a config for a concept like "routes" in an API Gateway where there are entries in "routes" for each service
 * being routed, and each service being routed has multiple properties that specify configuration for that route (stuff
 * like how and where to route for example).
 */
public class App {

  private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

  public static void main( String[] args ) {

    Injector injector = Guice.createInjector(new ConfigSupportModule("examples"));

    final App instance = injector.getInstance(App.class);

    instance.chainedConfigExample();
    instance.derivedConfigExample();
    instance.conditionalConfigExample();
    instance.configSubsetExample();
  }

  public App() {

  }

  public void chainedConfigExample() {
    DynamicIntProperty defaultServiceTimeout = new DynamicIntProperty("routes.default.serviceTimeout", 100);

    ChainedDynamicProperty.IntProperty exampleServiceTimeout = new ChainedDynamicProperty.IntProperty("routes.exampleService.serviceTimeout", defaultServiceTimeout);
    ChainedDynamicProperty.IntProperty otherServiceTimeout = new ChainedDynamicProperty.IntProperty("routes.otherService.serviceTimeout", defaultServiceTimeout);

    //values the same: 100
    LOGGER.info("example timeout: {}", exampleServiceTimeout.get());
    LOGGER.info("other timeout: {}", otherServiceTimeout.get());

    final AbstractConfiguration configInstance = ConfigurationManager.getConfigInstance();
    configInstance.setProperty("routes.default.serviceTimeout", 200);

    //values the same: 200
    LOGGER.info("example timeout: {}", exampleServiceTimeout.get());
    LOGGER.info("other timeout: {}", otherServiceTimeout.get());

    configInstance.setProperty("routes.exampleService.serviceTimeout", 500);

    //values different: 200, 500");
    LOGGER.info("example timeout: {}", exampleServiceTimeout.get());
    LOGGER.info("other timeout: {}", otherServiceTimeout.get());

    //callbacks aren't called on defaults
    exampleServiceTimeout.addCallback(() -> LOGGER.info("exampleServiceTimeout value changed to {}, ",
                                                        exampleServiceTimeout.getValue()));
    otherServiceTimeout.addCallback(() -> LOGGER.info("otherServiceTimeout value changed to {}, ",
                                                      otherServiceTimeout.getValue()));

    configInstance.setProperty("routes.default.serviceTimeout", 201);

    LOGGER.info("example timeout: {}", exampleServiceTimeout.get());
    LOGGER.info("other timeout: {}", otherServiceTimeout.get());
  }

  public void conditionalConfigExample() {
    final AbstractConfiguration configInstance = ConfigurationManager.getConfigInstance();
    LOGGER.info("The current environment name is {}", configInstance.getString("ZB_ENVIRONMENT_NAME"));

    String json =
      "["
        + "{"
        + "  \"if\": { \"ZB_ENVIRONMENT_NAME\": [\"test\"] },"
        + "  \"value\": 55"
        + "},"
        + "{"
        + "  \"if\": { \"ZB_ENVIRONMENT_NAME\": [\"local\"] },"
        + "  \"value\": 4"
        + "}"
      + "]";

    configInstance.setProperty("environmentSpecific", json);

    DynamicContextualProperty<Integer> prop = new DynamicContextualProperty<>("environmentSpecific", 0);

    LOGGER.info("the '{}' property currently is {}", prop.getName(), prop.getValue());
  }

  public void derivedConfigExample() {
    StringDerivedProperty<URL> urlProperty = new StringDerivedProperty<>("routes.exampleService.url",
                                                                         getUrlFromString("exampleService.local.zbdev.net"),
                                                                         this::getUrlFromString);

    LOGGER.info("the url is {}", urlProperty.getValue());
  }

  private URL getUrlFromString(String urlString) {
    //if it doesnt start with "<scheme>://" add the default scheme
    if (!urlString.matches("^\\w+://.*")) {
      urlString =  "https://" +urlString;
    }
    //if it doesn't end with ":/d+" add the default port
    if (!urlString.matches(".*:\\d+$")) {
      urlString = urlString + ":1234";
    }
    try {
      return new URL(urlString);
    }
    catch (MalformedURLException e) {
      throw new RuntimeException("Misconfigured route", e);
    }
  }

  public void configSubsetExample() {
    final AbstractConfiguration configInstance = ConfigurationManager.getConfigInstance();
    configInstance.setProperty("routes.exampleService", "this is an example service");
    configInstance.setProperty("routes.exampleService.serviceTimeout", 300);
    configInstance.setProperty("routes.exampleService.url", "https://www.example.com:2344");
    configInstance.setProperty("routes.exampleServiceThatIsBadlyNamed", "bad names are bad");

    final Configuration subset = configInstance.subset("routes.exampleService");
    subset.getKeys().forEachRemaining(prop-> LOGGER.info("found prop named '{}' with value '{}'", prop, subset.getProperty(prop)));
  }
}

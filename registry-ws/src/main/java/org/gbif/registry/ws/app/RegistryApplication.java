package org.gbif.registry.ws.app;

import org.gbif.api.service.registry.DatasetOccurrenceDownloadUsageService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NetworkService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.dropwizard.app.GbifBaseApplication;
import org.gbif.dropwizard.conf.PropertyKeyUtils;
import org.gbif.drupal.guice.DrupalMyBatisModule;
import org.gbif.registry.events.EventModule;
import org.gbif.registry.events.VarnishPurgeModule;
import org.gbif.registry.ims.ImsModule;
import org.gbif.registry.persistence.guice.RegistryMyBatisModule;
import org.gbif.registry.search.guice.RegistrySearchModule;
import org.gbif.registry.ws.app.conf.RegistryWsConfiguration;
import org.gbif.registry.ws.filter.AuthResponseCodeOverwriteFilter;
import org.gbif.registry.ws.filter.StaticContentFilter;
import org.gbif.registry.ws.guice.RegistryWsModule;
import org.gbif.registry.ws.guice.SecurityModule;
import org.gbif.registry.ws.guice.StringTrimInterceptor;
import org.gbif.registry.ws.resources.EnumerationResource;
import org.gbif.registry.ws.security.EditorAuthorizationFilter;
import org.gbif.registry.ws.security.LegacyAuthorizationFilter;

import java.util.EnumSet;
import java.util.Properties;

import javax.servlet.DispatcherType;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.bval.guice.ValidationModule;

public class RegistryApplication extends GbifBaseApplication<RegistryWsConfiguration> {

  public RegistryApplication() {
    super(true);
  }

  @Override
  public void initialize(Bootstrap<RegistryWsConfiguration> bootstrap) {
    bootstrap.addBundle(new AssetsBundle("/web"));
  }

  @Override
  public void run(
    RegistryWsConfiguration configuration, Environment environment
  ) throws Exception {
    environment.servlets().addFilter("Static content",StaticContentFilter.class).addMappingForServletNames(EnumSet.allOf(
      DispatcherType.class), true, "/*");
    super.run(configuration, environment);
  }

  @Override
  protected Class<?>[] getResourceClasses() {
    return new Class[] {LegacyAuthorizationFilter.class, EditorAuthorizationFilter.class,
      AuthResponseCodeOverwriteFilter.class, OrganizationService.class, DatasetOccurrenceDownloadUsageService.class,
      EnumerationResource.class, InstallationService.class, NetworkService.class, NodeService.class,
      OccurrenceDownloadService.class, DatasetService.class};
  }

  @Override
  protected Injector buildGuiceInjector(RegistryWsConfiguration configuration) {
    Properties properties = PropertyKeyUtils.toProperties(configuration);
    return Guice.createInjector(new RegistryWsModule(),new RegistryMyBatisModule(properties),
                                new DrupalMyBatisModule(properties),
                                new ImsModule(),
                                StringTrimInterceptor.newMethodInterceptingModule(),
                                new ValidationModule(),
                                new EventModule(properties),
                                new RegistrySearchModule(properties),
                                new SecurityModule(properties),
                                new VarnishPurgeModule(properties));
  }

  /**
   * Runs the application (required by Dropwizard).
   */
  public static void main(String[] args) throws Exception {
    new RegistryApplication().run(args);
  }
}

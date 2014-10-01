package org.gbif.registry.ws.guice;

import org.gbif.api.service.registry.DatasetOccurrenceDownloadUsageService;
import org.gbif.api.service.registry.DatasetSearchService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NetworkService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.ims.Augmenter;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.ContactMapper;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.DatasetOccurrenceDownloadMapper;
import org.gbif.registry.persistence.mapper.DatasetProcessStatusMapper;
import org.gbif.registry.persistence.mapper.EndpointMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.MetadataMapper;
import org.gbif.registry.persistence.mapper.MetasyncHistoryMapper;
import org.gbif.registry.persistence.mapper.NetworkMapper;
import org.gbif.registry.persistence.mapper.NodeMapper;
import org.gbif.registry.persistence.mapper.OccurrenceDownloadMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.ws.resources.DatasetOccurrenceDownloadUsageResource;
import org.gbif.registry.ws.resources.DatasetResource;
import org.gbif.registry.ws.resources.EnumerationResource;
import org.gbif.registry.ws.resources.InstallationResource;
import org.gbif.registry.ws.resources.NetworkResource;
import org.gbif.registry.ws.resources.NodeResource;
import org.gbif.registry.ws.resources.OccurrenceDownloadResource;
import org.gbif.registry.ws.resources.OrganizationResource;
import org.gbif.registry.ws.security.EditorAuthorizationService;
import org.gbif.registry.ws.security.LegacyAuthorizationFilter;
import org.gbif.ws.server.interceptor.NullToNotFound;
import org.gbif.ws.server.interceptor.NullToNotFoundInterceptor;

import javax.inject.Singleton;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.matcher.Matchers;

/**
 * Guice module that binds registry resources to be usable in DropWizard applications.
 * A provider method for each exposed resources is implemented to avoid proxy classes created by Guice when circular dependencies are detected.
 */
public class RegistryWsModule extends AbstractModule {

  @Override
  protected void configure() {
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(NullToNotFound.class), new NullToNotFoundInterceptor());
  }

  @Provides
  @Singleton
  protected EnumerationResource providesEnumerationResource(){
    return new EnumerationResource();
  }

  @Provides
  @Singleton
  protected DatasetService provideDatasetResource(
    DatasetMapper datasetMapper,
    ContactMapper contactMapper,
    EndpointMapper endpointMapper,
    MachineTagMapper machineTagMapper,
    TagMapper tagMapper,
    IdentifierMapper identifierMapper,
    CommentMapper commentMapper,
    EventBus eventBus,
    DatasetSearchService searchService,
    MetadataMapper metadataMapper,
    DatasetProcessStatusMapper datasetProcessStatusMapper,
    NetworkMapper networkMapper,
    EditorAuthorizationService userAuthService
  ) {
    return new DatasetResource(datasetMapper,
                               contactMapper,
                               endpointMapper,
                               machineTagMapper,
                               tagMapper,
                               identifierMapper,
                               commentMapper,
                               eventBus,
                               searchService,
                               metadataMapper,
                               datasetProcessStatusMapper,
                               networkMapper,
                               userAuthService);
  }

  @Provides
  @Singleton
  protected DatasetOccurrenceDownloadUsageService provideDatasetOccurrenceDownloadUsageResource(
    DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper
  ) {
    return new DatasetOccurrenceDownloadUsageResource(datasetOccurrenceDownloadMapper);
  }

  @Provides
  @Singleton
  protected InstallationService provideInstallationResource(
    InstallationMapper installationMapper,
    ContactMapper contactMapper,
    EndpointMapper endpointMapper,
    IdentifierMapper identifierMapper,
    MachineTagMapper machineTagMapper,
    TagMapper tagMapper,
    CommentMapper commentMapper,
    DatasetMapper datasetMapper,
    OrganizationMapper organizationMapper,
    MetasyncHistoryMapper metasyncHistoryMapper,
    EventBus eventBus,
    EditorAuthorizationService userAuthService
  ) {
    return new InstallationResource(installationMapper,
                                    contactMapper,
                                    endpointMapper,
                                    identifierMapper,
                                    machineTagMapper,
                                    tagMapper,
                                    commentMapper,
                                    datasetMapper,
                                    organizationMapper,
                                    metasyncHistoryMapper,
                                    eventBus,
                                    userAuthService);
  }

  @Provides
  @Singleton
  protected NetworkService provideNetworkResource(
    NetworkMapper networkMapper,
    ContactMapper contactMapper,
    EndpointMapper endpointMapper,
    IdentifierMapper identifierMapper,
    MachineTagMapper machineTagMapper,
    TagMapper tagMapper,
    CommentMapper commentMapper,
    DatasetMapper datasetMapper,
    EventBus eventBus,
    EditorAuthorizationService userAuthService
  ) {
    return new NetworkResource(networkMapper,
                               contactMapper,
                               endpointMapper,
                               identifierMapper,
                               machineTagMapper,
                               tagMapper,
                               commentMapper,
                               datasetMapper,
                               eventBus,
                               userAuthService);
  }

  @Provides
  @Singleton
  protected NodeService provideNodeResource(
    NodeMapper nodeMapper,
    IdentifierMapper identifierMapper,
    CommentMapper commentMapper,
    ContactMapper contactMapper,
    EndpointMapper endpointMapper,
    MachineTagMapper machineTagMapper,
    TagMapper tagMapper,
    OrganizationMapper organizationMapper,
    DatasetMapper datasetMapper,
    InstallationMapper installationMapper,
    EventBus eventBus,
    Augmenter nodeAugmenter,
    EditorAuthorizationService userAuthService
  ) {
    return new NodeResource(nodeMapper,
                            identifierMapper,
                            commentMapper,
                            contactMapper,
                            endpointMapper,
                            machineTagMapper,
                            tagMapper,
                            organizationMapper,
                            datasetMapper,
                            installationMapper,
                            eventBus,
                            nodeAugmenter,
                            userAuthService);
  }

  @Provides
  @Singleton
  protected OccurrenceDownloadService provideOccurrenceDownloadResource(OccurrenceDownloadMapper occurrenceDownloadMapper) {
    return new OccurrenceDownloadResource(occurrenceDownloadMapper);
  }

  @Provides
  @Singleton
  protected OrganizationService provideOccurrenceOrganizationResource(
    OrganizationMapper organizationMapper,
    ContactMapper contactMapper,
    EndpointMapper endpointMapper,
    MachineTagMapper machineTagMapper,
    TagMapper tagMapper,
    IdentifierMapper identifierMapper,
    CommentMapper commentMapper,
    DatasetMapper datasetMapper,
    InstallationMapper installationMapper,
    EventBus eventBus,
    EditorAuthorizationService userAuthService
  ) {
    return new OrganizationResource(organizationMapper,
                                    contactMapper,
                                    endpointMapper,
                                    machineTagMapper,
                                    tagMapper,
                                    identifierMapper,
                                    commentMapper,
                                    datasetMapper,
                                    installationMapper,
                                    eventBus,
                                    userAuthService);
  }

  @Provides
  LegacyAuthorizationFilter providesLegacyAuthorizationFilter(
    InstallationResource installationService, OrganizationResource organizationService, DatasetResource datasetService
  ) {
    return new LegacyAuthorizationFilter(installationService, organizationService, datasetService);
  }

}

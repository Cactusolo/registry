package org.gbif.registry.events;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.OrganizationService;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.ws.rs.core.UriBuilder;

import com.google.common.base.Joiner;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.http.annotation.NotThreadSafe;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A event bus listener that will flush varnish if registry entities like datasets and organizations have been
 * created, modified or deleted.
 * <p/>
 * Varnish provides two main ways of invalidating its cache: PURGE and BAN.
 * PURGE truly frees the cache, but only works on individual resource URLs while BANs work with regular expressions
 * and can banRegex entire subresources from being served. BANs do not remove the object from the varnish memory though.
 *
 * @see <h ref="https://www.varnish-software.com/static/book/Cache_invalidation.html">Varnish Book</h>
 *
 * <h3>Purging cascade logic</h3>
 * A quick overview of the logic how entity changes trigger purges on other resources.
 * In case of updates all keys used from the changed objects need be taken from both the old and new version.
 *
 * <h4>Node</h4>
 * <p>Nodes are only modified in the IMS and no events are ever triggered.</p>
 * <ul>
 *   <li>Node detail PURGE</li>
 *   <li>List all nodes BAN</li>
 * </ul>
 *
 * <h4>Organization</h4>
 * <ul>
 *   <li>Organization detail PURGE</li>
 *   <li>List all organizations BAN</li>
 *   <li>Publisher search & suggest BAN</li>
 *   <li>/node/{org.endorsingNodeKey}/organization</li>
 * </ul>
 *
 * <h4>Network</h4>
 * <ul>
 *   <li>Network detail PURGE</li>
 *   <li>List all networks BAN</li>
 * </ul>
 *
 * <h4>Installation</h4>
 * <ul>
 *   <li>Installation detail PURGE</li>
 *   <li>List all installations BAN</li>
 *   <li>/node/{i.organization.endorsingNodeKey}/installation BAN</li>
 *   <li>/organization/{UUID}/installation BAN</li>
 * </ul>
 *
 * <h4>Dataset</h4>
 * <ul>
 *   <li>dataset/{key} PURGE</li>
 *   <li>dataset/{d.parentKey} PURGE</li>
 *   <li>dataset/{d.parentKey}/constituents BAN</li>
 *   <li>dataset BAN</li>
 *   <li>dataset/search|suggest BAN</li>
 *   <li>/installation/{d.installationKey}/dataset BAN</li>
 *   <li>/organization/{d.installation.organizationKey}/hostedDataset BAN</li>
 *   <li>/organization/{d.publishingOrganizationKey}/publishedDataset BAN</li>
 *   <li>/node/{d.publishingOrganization.endorsingNodeKey}/dataset BAN</li>
 *   <li>/network/{any UUID}/constituents BAN</li>
 * </ul>
 *
 */
@Singleton
public class VarnishPurgeListener {
  private static final Logger LOG = LoggerFactory.getLogger(VarnishPurgeListener.class);
  private final CloseableHttpClient client;
  private final UriBuilder uriBuilder;
  private final String apiRoot;
  private final OrganizationService organizationService;
  private final InstallationService installationService;
  private final DatasetService datasetService;

  @Inject
  public VarnishPurgeListener(CloseableHttpClient client, EventBus eventBus, URI apiBaseUrl,
                              OrganizationService organizationService,InstallationService installationService,
                              DatasetService datasetService) {
    this.client = client;
    this.organizationService = organizationService;
    this.installationService = installationService;
    this.datasetService = datasetService;
    uriBuilder = UriBuilder.fromUri(apiBaseUrl);
    apiRoot = apiBaseUrl.getPath();
    eventBus.register(this);
  }

  @Subscribe
  public final <T extends NetworkEntity> void created(CreateEvent<T> event) {
    purgeEntityAndBanLists(event.getObjectClass(), event.getNewObject().getKey());

    if (event.getObjectClass().equals(Organization.class)) {
      cascadeOrganizationChange((Organization) event.getNewObject());
    } else if (event.getObjectClass().equals(Dataset.class)) {
      cascadeDatasetChange((Dataset) event.getNewObject());
    } else if (event.getObjectClass().equals(Installation.class)) {
      cascadeInstallationChange((Installation) event.getNewObject());
    }
  }

  @Subscribe
  public final <T extends NetworkEntity> void updated(UpdateEvent<T> event) {
    purgeEntityAndBanLists(event.getObjectClass(), event.getOldObject().getKey());

    if (event.getObjectClass().equals(Organization.class)) {
      cascadeOrganizationChange((Organization)event.getOldObject(), (Organization)event.getNewObject());
    } else if (event.getObjectClass().equals(Dataset.class)) {
      cascadeDatasetChange((Dataset)event.getOldObject(), (Dataset)event.getNewObject());
    } else if (event.getObjectClass().equals(Installation.class)) {
      cascadeInstallationChange((Installation)event.getOldObject(), (Installation)event.getNewObject());
    }
  }

  @Subscribe
  public final <T extends NetworkEntity> void deleted(DeleteEvent<T> event) {
    purgeEntityAndBanLists(event.getObjectClass(), event.getOldObject().getKey());

    if (event.getObjectClass().equals(Organization.class)) {
      cascadeOrganizationChange((Organization) event.getOldObject());
    } else if (event.getObjectClass().equals(Dataset.class)) {
      cascadeDatasetChange((Dataset) event.getOldObject());
    } else if (event.getObjectClass().equals(Installation.class)) {
      cascadeInstallationChange((Installation) event.getOldObject());
    }
  }

  @Subscribe
  public final void componentChange(ChangedComponentEvent event) {
    purgeEntityAndBanLists(event.getTargetClass(), event.getTargetEntityKey());
    // keys have not changed, only some component of the entity itself
    if (event.getTargetClass().equals(Organization.class)) {
      cascadeOrganizationChange(organizationService.get(event.getTargetEntityKey()));
    } else if (event.getTargetClass().equals(Dataset.class)) {
      cascadeDatasetChange(datasetService.get(event.getTargetEntityKey()));
    } else if (event.getTargetClass().equals(Installation.class)) {
      cascadeInstallationChange(installationService.get(event.getTargetEntityKey()));
    }
  }

  // group bans by entity class to avoid too many ban rules and thus bad varnish performance
  private void cascadeDatasetChange(Dataset ... datasets) {
    // UUIDHashSet ignores null values
    Set<UUID> instKeys = new UUIDHashSet();
    Set<UUID> orgKeys = new UUIDHashSet();
    Set<UUID> nodeKeys = new UUIDHashSet();
    Set<UUID> parentKeys = new UUIDHashSet();
    for (Dataset d : datasets) {
      if (!orgKeys.contains(d.getPublishingOrganizationKey())) {
        Organization o = organizationService.get(d.getPublishingOrganizationKey());
        nodeKeys.add(o.getEndorsingNodeKey());
      }
      if (!instKeys.contains(d.getInstallationKey())) {
        instKeys.add(d.getInstallationKey());
        Installation i = installationService.get(d.getInstallationKey());
        orgKeys.add(i.getOrganizationKey());
      }
      orgKeys.add(d.getPublishingOrganizationKey());
      if (d.getParentDatasetKey() != null) {
        parentKeys.add(d.getParentDatasetKey());
        purge(uriBuilder.path("dataset").path(d.getParentDatasetKey().toString()).build());
      }
    }
    banRegex(String.format("%s/dataset/%s/constituents", apiRoot, anyKey(parentKeys)));
    // /installation/{d.installationKey}/dataset BAN
    banRegex(String.format("%s/installation/%s/dataset", apiRoot, anyKey(instKeys)));
    // /organization/{d.publishingOrganizationKey}/publishedDataset BAN
    // /organization/{d.installation.organizationKey}/hostedDataset BAN
    banRegex(String.format("%s/organization/%s/(published|hosted)Dataset", apiRoot, anyKey(orgKeys)));
    // /node/{d.publishingOrganization.endorsingNodeKey}/dataset BAN
    banRegex(String.format("%s/node/%s/dataset", apiRoot, anyKey(nodeKeys)));
    // /network/{any UUID}/constituents BAN
    banRegex(String.format("%s/network/.+/constituents", apiRoot));
  }

  private void cascadeOrganizationChange(Organization ... orgs) {
    // /node/{org.endorsingNodeKey}/
    Set<UUID> nodeKeys = new UUIDHashSet();
    for (Organization o : orgs) {
      nodeKeys.add(o.getEndorsingNodeKey());
    }
    banRegex(String.format("%s/node/%s/organization", apiRoot, anyKey(nodeKeys)));
  }

  private void cascadeInstallationChange(Installation ... installations) {
    Set<UUID> keys = new UUIDHashSet();
    // /organization/{i.organizationKey}/installation BAN
    for (Installation i : installations) {
      keys.add(i.getOrganizationKey());
    }
    banRegex(String.format("%s/organization/%s/installation", apiRoot, anyKey(keys)));

    // /node/{i.organization.endorsingNodeKey}/installation BAN
    Set<UUID> nodekeys = new UUIDHashSet();
    for (UUID orgKey : keys) {
      Organization o = organizationService.get(orgKey);
      nodekeys.add(o.getEndorsingNodeKey());
    }
    banRegex(String.format("%s/node/%s/organization", apiRoot, anyKey(nodekeys)));
  }

  private String anyKey(Set<UUID> keys) {
    if (keys.size() == 1) {
      return keys.iterator().next().toString();
    }
    return "(" + Joiner.on("|").skipNulls().join(keys) + ")";
  }
  /**
   * Removes the specific entity from varnish and bans search & list pages.
   * This method does not check which entity class was supplied, but as it is some type of NetworkEntity
   * we deal with the right urls.
   */
  private void purgeEntityAndBanLists(Class cl, UUID key) {
    // purge entity detail
    purge(uriBuilder.path(cl.getSimpleName().toLowerCase()).path(key.toString()).build());

    // banRegex lists and searches
    banRegex(String.format("%s/%s(/search|/suggest)?[^/]*$", apiRoot, cl.getSimpleName().toLowerCase()));
  }

  private void purge(URI uri) {
    try {
      CloseableHttpResponse resp = client.execute(new HttpPurge(uri));
      resp.close();
    } catch (IOException e) {
      LOG.error("Failed to purge {}", uri, e);
    }
  }

  private void banRegex(String regex) {
    try {
      CloseableHttpResponse resp = client.execute(new HttpBan(regex));
      resp.close();
    } catch (IOException e) {
      LOG.error("Failed to ban {}", regex, e);
    }
  }

  /**
   * The HTTP PURGE method is used by varnish to flush its cache via http.
   */
  @NotThreadSafe
  public class HttpPurge extends HttpRequestBase {

    public static final String METHOD_NAME = "PURGE";


    public HttpPurge(final URI uri) {
      super();
      setURI(uri);
    }

    @Override
    public String getMethod() {
      return METHOD_NAME;
    }

  }

  /**
   * The HTTP BAN method is used by varnish to flush its cache via http.
   */
  @NotThreadSafe
  public class HttpBan extends HttpRequestBase {

    public static final String METHOD_NAME = "BAN";
    public static final String BAN_HEADER = "x-ban-url";

    public HttpBan(String banRegex) {
      super();
      setURI(uriBuilder.build());
      setHeader(BAN_HEADER, banRegex);
    }

    @Override
    public String getMethod() {
      return METHOD_NAME;
    }

  }

  /**
   * HashSet with an overriden add method that silently ignores null values being added.
   */
  public class UUIDHashSet extends HashSet<UUID> {
    @Override
    public boolean add(UUID t) {
      if (t != null) {
        return super.add(t);
      }
      return false;
    }
  }
}

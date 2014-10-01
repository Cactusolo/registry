package org.gbif.registry.ws.app.conf;

import org.gbif.dropwizard.conf.DiscoverableServiceConfiguration;
import org.gbif.dropwizard.conf.JdbcConfiguration;
import org.gbif.dropwizard.conf.MailConfiguration;
import org.gbif.dropwizard.conf.PostalServiceConfiguration;
import org.gbif.dropwizard.conf.PropertiesKey;
import org.gbif.dropwizard.conf.PropertyKeyUtils;
import org.gbif.dropwizard.conf.SolrConfiguration;

import javax.validation.Valid;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import com.google.common.io.Resources;

public class RegistryWsConfiguration extends DiscoverableServiceConfiguration {

  @PropertiesKey("registry")
  private JdbcConfiguration db;

  @PropertiesKey("registry.search.performIndexSync")
  private boolean performIndexSync = true;

  // file with all application keys & secrets
  @PropertiesKey("appkeys.file")
  private String appkeysFile;

  @PropertiesKey("api.url")
  private String apiUrl;

  @PropertiesKey("drupal")
  @Valid
  private JdbcConfiguration drupal;

  @PropertiesKey("registry.postalservice")
  @Valid
  private PostalServiceConfiguration postalService;


  @PropertiesKey("registry.sandboxmode.enabled")
  private boolean sandboxMode;

  @PropertiesKey("mail.devemail")
  private String devEmail;

  @PropertiesKey("mail.devemail.enabled")
  private boolean  devEmailEnabled;

  @PropertiesKey
  @Valid
  private MailConfiguration mail;

  @PropertiesKey
  @Valid
  private SolrConfiguration solr;

  @JsonProperty
  public JdbcConfiguration getDb() {
    return db;
  }

  public void setDb(JdbcConfiguration db) {
    this.db = db;
  }

  @JsonProperty
  public boolean getPerformIndexSync() {
    return performIndexSync;
  }

  public void setPerformIndexSync(boolean performIndexSync) {
    this.performIndexSync = performIndexSync;
  }

  @JsonProperty
  public String getAppkeysFile() {
    return appkeysFile;
  }

  public void setAppkeysFile(String appkeysFile) {
    this.appkeysFile = appkeysFile;
  }

  @JsonProperty
  public String getApiUrl() {
    return apiUrl;
  }

  public void setApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
  }

  @JsonProperty
  public JdbcConfiguration getDrupal() {
    return drupal;
  }

  public void setDrupal(JdbcConfiguration drupal) {
    this.drupal = drupal;
  }

  @JsonProperty
  public PostalServiceConfiguration getPostalService() {
    return postalService;
  }

  public void setPostalService(PostalServiceConfiguration postalService) {
    this.postalService = postalService;
  }

  @JsonProperty
  public boolean getSandboxMode() {
    return sandboxMode;
  }

  public void setSandboxMode(boolean sandboxMode) {
    this.sandboxMode = sandboxMode;
  }

  @JsonProperty
  public String getDevEmail() {
    return devEmail;
  }

  public void setDevEmail(String devEmail) {
    this.devEmail = devEmail;
  }

  @JsonProperty
  public boolean getDevEmailEnabled() {
    return devEmailEnabled;
  }

  public void setDevEmailEnabled(boolean devEmailEnabled) {
    this.devEmailEnabled = devEmailEnabled;
  }

  @JsonProperty
  public MailConfiguration getMail() {
    return mail;
  }

  public void setMail(MailConfiguration mail) {
    this.mail = mail;
  }

  @JsonProperty
  public SolrConfiguration getSolr() {
    return solr;
  }

  public void setSolr(SolrConfiguration solr) {
    this.solr = solr;
  }

}

package org.gbif.registry.doi;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.DoiException;
import org.gbif.doi.service.DoiService;
import org.gbif.doi.service.datacite.DataCiteValidator;

import java.net.URI;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

/**
 * Class that implements a mock doi service that validates metadata xml.
 */
public class DoiServiceMock implements DoiService {
  public static final URI MOCK_TARGET = URI.create("http://www.gbif.org");

  @Nullable
  @Override
  public DoiData resolve(DOI doi) throws DoiException {
    return new DoiData(DoiStatus.REGISTERED, MOCK_TARGET);
  }

  @Override
  public void reserve(DOI doi, String metadata) throws DoiException {
    DataCiteValidator.validateMetadata(metadata);
  }

  @Override
  public void reserve(DOI doi, DataCiteMetadata metadata) throws DoiException {
    DataCiteValidator.toXml(doi, metadata);
  }

  @Override
  public void register(DOI doi, URI target, String metadata) throws DoiException {
    Preconditions.checkNotNull(doi);
    Preconditions.checkNotNull(target);
    DataCiteValidator.validateMetadata(metadata);
  }

  @Override
  public void register(DOI doi, URI target, DataCiteMetadata metadata) throws DoiException {
    Preconditions.checkNotNull(doi);
    DataCiteValidator.toXml(doi, metadata);
    Preconditions.checkNotNull(target);
  }

  @Override
  public boolean delete(DOI doi) throws DoiException {
    Preconditions.checkNotNull(doi);
    // dont do nothing
    return false;
  }

  @Override
  public void update(DOI doi, String metadata) throws DoiException {
    Preconditions.checkNotNull(doi);
    DataCiteValidator.validateMetadata(metadata);
  }

  @Override
  public void update(DOI doi, DataCiteMetadata metadata) throws DoiException {
    DataCiteValidator.toXml(doi, metadata);
  }

  @Override
  public void update(DOI doi, URI target) throws DoiException {
    Preconditions.checkNotNull(doi);
    Preconditions.checkNotNull(target);
  }

}

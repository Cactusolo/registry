package org.gbif.registry.cli;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.common.messaging.AbstractMessageCallback;
import org.gbif.common.messaging.api.messages.ChangeDoiMessage;
import org.gbif.doi.service.DoiException;
import org.gbif.doi.service.DoiExistsException;
import org.gbif.doi.service.DoiHttpException;
import org.gbif.doi.service.DoiService;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.registry.persistence.mapper.DoiMapper;
import org.gbif.registry.ws.util.DataCiteConverter;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Message callback implementation to take DOI updates and send them to DataCite. Updates the status of the DOI in
 * the registry database.
 */
public class DoiUpdateListener extends AbstractMessageCallback<ChangeDoiMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(DoiUpdateListener.class);
  private static final Marker DOI_SMTP = MarkerFactory.getMarker("DOI_SMTP");
  private final long timeToRetryInMs;

  private final DoiService doiService;
  private final DoiMapper doiMapper;

  public DoiUpdateListener(DoiService doiService, DoiMapper doiMapper, long timeToRetryInMs) {
    this.doiService = doiService;
    this.doiMapper = doiMapper;
    this.timeToRetryInMs = timeToRetryInMs;
  }

  @Override
  public void handleMessage(ChangeDoiMessage msg) {
    LOG.debug("Handling change DOI to {} message for {}", msg.getStatus(), msg.getDoi());
    final DoiData currState = doiMapper.get(msg.getDoi());
    if (currState == null) {
      // this is bad, we should have an entry for the DOI in our registry table!
      LOG.warn("Skipping unknown GBIF DOI {}", msg.getDoi());
      return;
    }

    for (int retry = 1; retry < 4; retry++){
      try {
        switch (msg.getStatus()) {
          case REGISTERED:
            registerOrUpdate(msg.getDoi(), msg.getTarget(), msg.getMetadata(), currState);
            break;
          case RESERVED:
            reserve(msg.getDoi(), msg.getMetadata(), currState);
            break;
          case DELETED:
            delete(msg.getDoi(), currState);
            break;
          default:
            LOG.warn("Cannot update {} to illegal state {}.", msg.getDoi(), msg.getStatus());
            break;
        }
        break;

      } catch (DoiExistsException e) {
        writeFailedStatus(msg.getDoi(), msg.getTarget(), msg.getMetadata());
        LOG.warn(DOI_SMTP, "DOI {} existed already when trying to change status to {}. Ignore", msg.getDoi(), msg.getStatus(), e);
        break;

      } catch (DoiHttpException e) {
        writeFailedStatus(msg.getDoi(), msg.getTarget(), msg.getMetadata());
        if (e.getStatus() == 413) {
          LOG.warn(DOI_SMTP, "Metadata of length {} is exceeding max datacite limit in attempt #{} "
                             + "while updating {} to {} with target {}. "
                             + "Trying again with truncated metadata", msg.getMetadata().length(), retry,
            msg.getDoi(), msg.getStatus(), msg.getTarget(), e);
          try {
            LOG.debug("Original metadata for DOI {}:\n\n{}", msg.getDoi(), msg.getMetadata());
            String truncatedXml = DataCiteConverter.truncateDescription(msg.getDoi(), msg.getMetadata(), msg.getTarget());
            msg = new ChangeDoiMessage(msg.getStatus(), msg.getDoi(), truncatedXml, msg.getTarget());
          } catch (InvalidMetadataException e1) {
            LOG.warn("Failed to deserialize xml metadata for DOI {}", msg.getDoi(), e1);
          }
        } else {
          LOG.warn(DOI_SMTP, "DOI http {} exception updating {} to {} with target {}. Attempt #{}",
            e.getStatus(), msg.getDoi(), msg.getStatus(), msg.getTarget(), retry, e);
          sleep();
        }

      } catch (DoiException e) {
        writeFailedStatus(msg.getDoi(), msg.getTarget(), msg.getMetadata());
        LOG.warn(DOI_SMTP, "DOI exception updating {} to {} with target {}. Attempt #{}",
          msg.getDoi(), msg.getStatus(), msg.getTarget(), retry, e);
        sleep();
      }
    }
  }

  private void sleep() {
    try {
      Thread.sleep(timeToRetryInMs);
    } catch (InterruptedException e1) {
      LOG.info("Interrupted retries");
    }
  }

  private void writeFailedStatus(DOI doi, URI target, String xml) {
    doiMapper.update(doi, new DoiData(DoiStatus.FAILED, target), xml);
  }

  private void reserve(DOI doi, String xml, DoiData currState) throws DoiException {
    doiService.reserve(doi, xml);
    LOG.info("Reserved doi {}", doi);
    DoiData newState = new DoiData(DoiStatus.RESERVED, currState.getTarget());
    doiMapper.update(doi, newState, xml);
  }

  private void delete(DOI doi, DoiData currState) throws DoiException {
    if (currState.getStatus() == null) {
      doiMapper.delete(doi);
    } else {
      try {
        boolean fullDeleted = doiService.delete(doi);
        if (fullDeleted) {
          doiMapper.delete(doi);
          LOG.info("Deleted doi {}", doi);
        } else {
          DoiData newState = new DoiData(DoiStatus.DELETED, currState.getTarget());
          doiMapper.update(doi, newState, null);
          LOG.info("Marked registered doi {} as deleted", doi);
        }
      } catch (DoiHttpException e) {
        // in case of a 404 swallow
        if (e.getStatus() == 404) {
          LOG.warn(DOI_SMTP, "Trying to delete DOI {} failed because it doesn't exist in DataCite; deleting locally",
            doi, e);
          doiMapper.delete(doi);
        } else {
          throw e;
        }
      }
    }
  }

  private void registerOrUpdate(DOI doi, URI target, String xml, DoiData currState) throws DoiException {
    if (DoiStatus.REGISTERED == currState.getStatus()) {
      // the DOI was already registered, so we only need to update the target url if changed and the metadata
      if (!target.equals(currState.getTarget())) {
        doiService.update(doi, target);
      }
      doiService.update(doi, xml);
      LOG.info("Updated doi {} with target {}", doi, target);
    } else {
      doiService.register(doi, target, xml);
      LOG.info("Registered doi {} with target {}", doi, target);
    }
    // store the new state in our registry
    doiMapper.update(doi, new DoiData(DoiStatus.REGISTERED, target), xml);
  }
}

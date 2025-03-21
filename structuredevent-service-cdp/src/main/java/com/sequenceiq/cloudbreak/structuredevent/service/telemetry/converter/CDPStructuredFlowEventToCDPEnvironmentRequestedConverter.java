package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;

@Component
public class CDPStructuredFlowEventToCDPEnvironmentRequestedConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPStructuredFlowEventToCDPEnvironmentRequestedConverter.class);

    @Inject
    private CDPStructuredFlowEventToCDPOperationDetailsConverter operationDetailsConverter;

    @Inject
    private EnvironmentDetailsToCDPEnvironmentDetailsConverter environmentDetailsConverter;

    @Inject
    private EnvironmentDetailsToCDPFreeIPADetailsConverter freeIPADetailsConverter;

    @Inject
    private EnvironmentDetailsToCDPEnvironmentTelemetryFeatureDetailsConverter telemetryFeatureDetailsConverter;

    public UsageProto.CDPEnvironmentRequested convert(CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent) {
        UsageProto.CDPEnvironmentRequested.Builder cdpEnvironmentRequestedBuilder = UsageProto.CDPEnvironmentRequested.newBuilder();

        cdpEnvironmentRequestedBuilder.setOperationDetails(operationDetailsConverter.convert(cdpStructuredFlowEvent));

        if (cdpStructuredFlowEvent != null) {
            EnvironmentDetails environmentDetails = cdpStructuredFlowEvent.getPayload();
            cdpEnvironmentRequestedBuilder.setEnvironmentDetails(environmentDetailsConverter.convert(environmentDetails));
            cdpEnvironmentRequestedBuilder.setTelemetryFeatureDetails(telemetryFeatureDetailsConverter.convert(environmentDetails));
            cdpEnvironmentRequestedBuilder.setFreeIPA(freeIPADetailsConverter.convert(environmentDetails));
        }

        UsageProto.CDPEnvironmentRequested ret = cdpEnvironmentRequestedBuilder.build();
        LOGGER.debug("Converted CDPEnvironmentRequested event: {}", ret);
        return ret;
    }
}

package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum ClusterUpgradeValidationHandlerSelectors implements FlowEvent {

    VALIDATE_DISK_SPACE_EVENT,
    VALIDATE_SERVICES_EVENT,
    VALIDATE_IMAGE_EVENT;

    @Override
    public String event() {
        return name();
    }

}

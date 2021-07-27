package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CDPServicePolicyVerificationResponses;

public class CDPServicePolicyVerificationResult extends CloudPlatformResult {

    private CDPServicePolicyVerificationResponses cdpServicePolicyVerificationResponses;

    public CDPServicePolicyVerificationResult(Long resourceId, CDPServicePolicyVerificationResponses cloudCredentialStatus) {
        super(resourceId);
        this.cdpServicePolicyVerificationResponses = cloudCredentialStatus;
    }

    public CDPServicePolicyVerificationResult(String statusReason, Exception errorDetails, Long resourceId,
        CDPServicePolicyVerificationResponses cloudCredentialStatus) {
        super(statusReason, errorDetails, resourceId);
        this.cdpServicePolicyVerificationResponses = cloudCredentialStatus;
    }

    public CDPServicePolicyVerificationResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public CDPServicePolicyVerificationResponses getCdpServicePolicyVerificationResponses() {
        return cdpServicePolicyVerificationResponses;
    }
}

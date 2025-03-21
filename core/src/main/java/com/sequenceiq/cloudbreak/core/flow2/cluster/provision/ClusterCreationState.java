package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;

public enum ClusterCreationState implements FlowState {
    INIT_STATE,
    CLUSTER_PROXY_REGISTRATION_STATE,
    CLUSTER_CREATION_FAILED_STATE,
    BOOTSTRAPPING_MACHINES_STATE,
    COLLECTING_HOST_METADATA_STATE,
    VALIDATE_CLOUD_STORAGE_STATE,
    CLEANUP_FREEIPA_STATE,
    UPLOAD_RECIPES_STATE,
    BOOTSTRAPPING_PUBLIC_ENDPOINT_STATE,
    BOOTSTRAPPING_FREEIPA_ENDPOINT_STATE,
    CONFIGURE_KEYTABS_STATE,
    STARTING_CLUSTER_MANAGER_SERVICES_STATE,
    PREFLIGHT_CHECK_STATE,
    STARTING_CLUSTER_MANAGER_STATE,
    CONFIGURE_LDAP_SSO_STATE,
    WAIT_FOR_CLUSTER_MANAGER_STATE,
    EXECUTE_POST_CLUSTER_MANAGER_START_RECIPES_STATE,
    PREPARE_PROXY_CONFIG_STATE,
    SETUP_MONITORING_STATE,
    PREPARE_EXTENDED_TEMPLATE_STATE,
    VALIDATE_LICENCE_STATE,
    CONFIGURE_MANAGEMENT_SERVICES_STATE,
    CONFIGURE_SUPPORT_TAGS_STATE,
    UPDATE_CONFIG_STATE,
    REFRESH_PARCEL_REPOS_STATE,
    INSTALLING_CLUSTER_STATE,
    AUTOCONFIGURE_CLUSTER_MANAGER_STATE,
    START_MANAGEMENT_SERVICES_STATE,
    SUPPRESS_WARNINGS_STATE,
    CONFIGURE_KERBEROS_STATE,
    EXECUTE_POST_INSTALL_RECIPES_STATE,
    PREPARE_DATALAKE_RESOURCE_STATE,
    FINALIZE_CLUSTER_INSTALL_STATE,
    CLUSTER_CREATION_FINISHED_STATE,
    CLUSTER_PROXY_GATEWAY_REGISTRATION_STATE,
    FINAL_STATE;

    private final Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}

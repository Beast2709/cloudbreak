package com.sequenceiq.datalake.entity;

import com.sequenceiq.cloudbreak.event.ResourceEvent;

public enum DatalakeStatusEnum {

    REQUESTED(ResourceEvent.SDX_CLUSTER_PROVISION_STARTED),
    WAIT_FOR_ENVIRONMENT(ResourceEvent.SDX_WAITING_FOR_ENVIRONMENT),
    ENVIRONMENT_CREATED(ResourceEvent.SDX_ENVIRONMENT_FINISHED),
    STACK_CREATION_IN_PROGRESS(ResourceEvent.SDX_CLUSTER_PROVISION_STARTED),
    STACK_CREATION_FINISHED(ResourceEvent.SDX_CLUSTER_PROVISION_FINISHED),
    STACK_DELETED(ResourceEvent.SDX_CLUSTER_DELETED),
    STACK_DELETION_IN_PROGRESS(ResourceEvent.SDX_CLUSTER_DELETION_STARTED),
    EXTERNAL_DATABASE_CREATION_IN_PROGRESS(ResourceEvent.SDX_RDS_CREATION_STARTED),
    EXTERNAL_DATABASE_CREATED(ResourceEvent.SDX_RDS_CREATION_FINISHED),
    EXTERNAL_DATABASE_DELETION_IN_PROGRESS(ResourceEvent.SDX_RDS_DELETION_STARTED),
    EXTERNAL_DATABASE_START_IN_PROGRESS(ResourceEvent.SDX_RDS_START_STARTED),
    EXTERNAL_DATABASE_STARTED(ResourceEvent.SDX_RDS_START_FINISHED),
    EXTERNAL_DATABASE_STOP_IN_PROGRESS(ResourceEvent.SDX_RDS_STOP_STARTED),
    EXTERNAL_DATABASE_STOPPED(ResourceEvent.SDX_RDS_STOP_FINISHED),
    RUNNING(ResourceEvent.SDX_CLUSTER_CREATED),
    PROVISIONING_FAILED(ResourceEvent.SDX_CLUSTER_CREATION_FAILED),
    REPAIR_IN_PROGRESS(ResourceEvent.SDX_REPAIR_STARTED),
    REPAIR_FAILED(ResourceEvent.SDX_REPAIR_FAILED),
    CHANGE_IMAGE_IN_PROGRESS(ResourceEvent.SDX_CHANGE_IMAGE_STARTED),
    DATALAKE_UPGRADE_IN_PROGRESS(ResourceEvent.DATALAKE_UPGRADE_STARTED),
    DATALAKE_UPGRADE_FAILED(ResourceEvent.DATALAKE_UPGRADE_FAILED),
    DELETE_REQUESTED(ResourceEvent.SDX_CLUSTER_DELETION_STARTED),
    DELETED(ResourceEvent.SDX_CLUSTER_DELETION_FINISHED),
    DELETE_FAILED(ResourceEvent.SDX_CLUSTER_DELETION_FAILED),
    DELETED_ON_PROVIDER_SIDE(ResourceEvent.SDX_CLUSTER_DELETED_ON_PROVIDER_SIDE),
    START_IN_PROGRESS(ResourceEvent.SDX_START_STARTED),
    START_FAILED(ResourceEvent.SDX_START_FAILED),
    STOP_IN_PROGRESS(ResourceEvent.SDX_STOP_STARTED),
    STOP_FAILED(ResourceEvent.SDX_STOP_FAILED),
    STOPPED(ResourceEvent.SDX_STOP_FINISHED),
    CLUSTER_AMBIGUOUS(ResourceEvent.CLUSTER_AMBARI_CLUSTER_SYNCHRONIZED),
    SYNC_FAILED(ResourceEvent.SDX_SYNC_FAILED),
    CERT_ROTATION_IN_PROGRESS(ResourceEvent.SDX_CERT_ROTATION_STARTED),
    CERT_ROTATION_FAILED(ResourceEvent.SDX_CERT_ROTATION_FAILED),
    CERT_ROTATION_FINISHED(ResourceEvent.SDX_CERT_ROTATION_FINISHED),
    CERT_RENEWAL_IN_PROGRESS(ResourceEvent.DATALAKE_CERT_RENEWAL_STARTED),
    CERT_RENEWAL_FAILED(ResourceEvent.DATALAKE_CERT_RENEWAL_FAILED),
    CERT_RENEWAL_FINISHED(ResourceEvent.DATALAKE_CERT_RENEWAL_FINISHED),
    DATALAKE_BACKUP_INPROGRESS(ResourceEvent.DATALAKE_BACKUP_IN_PROGRESS),
    DATALAKE_RESTORE_INPROGRESS(ResourceEvent.DATALAKE_RESTORE_IN_PROGRESS),
    DATALAKE_RESTORE_FAILED(ResourceEvent.DATALAKE_RESTORE_FAILED),
    DATALAKE_DETACHED(ResourceEvent.DATALAKE_DETACHED);

    private ResourceEvent resourceEvent;

    DatalakeStatusEnum(ResourceEvent resourceEvent) {
        this.resourceEvent = resourceEvent;
    }

    public boolean isDeleteInProgressOrCompleted() {
        return EXTERNAL_DATABASE_DELETION_IN_PROGRESS.equals(this)
                || STACK_DELETED.equals(this)
                || STACK_DELETION_IN_PROGRESS.equals(this)
                || DELETE_REQUESTED.equals(this)
                || DELETED.equals(this)
                || DELETE_FAILED.equals(this);
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    public DatalakeStatusEnum mapToFailedIfInProgress() {
        switch (this) {
            case START_IN_PROGRESS:
                return START_FAILED;
            case STOP_IN_PROGRESS:
                return STOP_FAILED;
            case REQUESTED:
            case WAIT_FOR_ENVIRONMENT:
            case STACK_CREATION_IN_PROGRESS:
            case ENVIRONMENT_CREATED:
            case EXTERNAL_DATABASE_CREATION_IN_PROGRESS:
                return PROVISIONING_FAILED;
            case STACK_DELETION_IN_PROGRESS:
            case EXTERNAL_DATABASE_DELETION_IN_PROGRESS:
            case DELETE_REQUESTED:
                return DELETE_FAILED;
            case REPAIR_IN_PROGRESS:
                return REPAIR_FAILED;
            case CHANGE_IMAGE_IN_PROGRESS:
            case DATALAKE_UPGRADE_IN_PROGRESS:
                return DATALAKE_UPGRADE_FAILED;
            case CERT_ROTATION_IN_PROGRESS:
                return CERT_ROTATION_FAILED;
            default:
                return this;
        }
    }

    public ResourceEvent getDefaultResourceEvent() {
        return resourceEvent;
    }

}

package com.sequenceiq.freeipa.flow.chain;

import static com.sequenceiq.freeipa.flow.freeipa.backup.full.FullBackupEvent.FULL_BACKUP_EVENT;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_CHANGE_EVENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.freeipa.flow.freeipa.backup.full.event.TriggerFullBackupEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.ChangePrimaryGatewayEvent;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateEvent;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateTriggerEvent;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.action.SaltUpdateActions;
import com.sequenceiq.freeipa.flow.freeipa.upgrade.UpgradeEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleEvent;
import com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvent;

@Component
public class UpgradeFlowEventChainFactory implements FlowEventChainFactory<UpgradeEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltUpdateActions.class);

    private static final int PRIMARY_GW_EVENT_COUNT = 3;

    private static final int PRIMARY_GW_INSTANCE_COUNT = 1;

    @Override
    public String initEvent() {
        return FlowChainTriggers.UPGRADE_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(UpgradeEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new SaltUpdateTriggerEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted(), true, false)
                .withOperationId(event.getOperationId()));
        createBackupTriggerEvent(event).ifPresent(flowEventChain::add);
        flowEventChain.add(new ImageChangeEvent(IMAGE_CHANGE_EVENT.event(), event.getResourceId(), event.getImageSettingsRequest())
                .withOperationId(event.getOperationId()));

        int nonPrimaryGwInstanceCount = event.getInstanceIds().size();
        int instanceCountForUpscale = nonPrimaryGwInstanceCount + PRIMARY_GW_INSTANCE_COUNT + 1;
        int instanceCountForDownscale = nonPrimaryGwInstanceCount + PRIMARY_GW_INSTANCE_COUNT;
        flowEventChain.addAll(createScaleEventsForNonPgwInstances(event, instanceCountForUpscale, instanceCountForDownscale));
        flowEventChain.addAll(createScaleEventsAndChangePgw(event, instanceCountForUpscale, instanceCountForDownscale));
        flowEventChain.add(new SaltUpdateTriggerEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted(), true, true)
                .withOperationId(event.getOperationId()));
        return new FlowTriggerEventQueue(getName(), flowEventChain);
    }

    private List<Selectable> createScaleEventsAndChangePgw(UpgradeEvent event, int instanceCountForUpscale, int instanceCountForDownscale) {
        LOGGER.debug("Add events for primary gateway with id: [{}]", event.getPrimareGwInstanceId());
        List<Selectable> events = new ArrayList<>(PRIMARY_GW_EVENT_COUNT);
        events.add(new UpscaleEvent(UpscaleFlowEvent.UPSCALE_EVENT.event(),
                event.getResourceId(), instanceCountForUpscale, Boolean.FALSE, true, false, event.getOperationId()));
        List<String> oldInstances = new ArrayList<>(event.getInstanceIds());
        oldInstances.add(event.getPrimareGwInstanceId());
        events.add(new ChangePrimaryGatewayEvent(ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_EVENT.event(), event.getResourceId(),
                oldInstances, Boolean.FALSE, event.getOperationId()));
        events.add(new DownscaleEvent(DownscaleFlowEvent.DOWNSCALE_EVENT.event(),
                event.getResourceId(), List.of(event.getPrimareGwInstanceId()), instanceCountForDownscale, false, true, false, event.getOperationId()));
        return events;
    }

    private List<Selectable> createScaleEventsForNonPgwInstances(UpgradeEvent event, int instanceCountForUpscale, int instanceCountForDownscale) {
        LOGGER.debug("Add scale events for non primary gateway instances. upscale count: [{}] downscale count: [{}]",
                instanceCountForUpscale, instanceCountForDownscale);
        List<Selectable> events = new ArrayList<>(event.getInstanceIds().size() * 2);
        for (String instanceId : event.getInstanceIds()) {
            LOGGER.debug("Add upscale and downscale event for [{}]", instanceId);
            events.add(new UpscaleEvent(UpscaleFlowEvent.UPSCALE_EVENT.event(),
                    event.getResourceId(), instanceCountForUpscale, Boolean.FALSE, true, false, event.getOperationId()));
            events.add(new DownscaleEvent(DownscaleFlowEvent.DOWNSCALE_EVENT.event(),
                    event.getResourceId(), List.of(instanceId), instanceCountForDownscale, false, true, false, event.getOperationId()));
        }
        return events;
    }

    private Optional<TriggerFullBackupEvent> createBackupTriggerEvent(UpgradeEvent event) {
        if (event.isBackupSet()) {
            LOGGER.info("Backup is set, adding full backup to upgrade flow chain");
            return Optional.of(new TriggerFullBackupEvent(FULL_BACKUP_EVENT.event(), event.getResourceId(), event.getOperationId(), true, false));
        } else {
            LOGGER.info("Backup is not set for FreeIPA. No backup will be performed before upgrade.");
            return Optional.empty();
        }
    }

    @Override
    public String getName() {
        return "FreeIPA upgrade flow";
    }
}

package com.sequenceiq.cloudbreak.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.StackInstanceStatusChecker;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackSyncService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.flow.core.FlowLogService;

import io.opentracing.Tracer;

@RunWith(MockitoJUnitRunner.class)
public class StackStatusCheckerJobTest {

    @InjectMocks
    private StackStatusCheckerJob underTest;

    @Mock
    private StatusCheckerJobService jobService;

    @Mock
    private StackService stackService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private ClusterOperationService clusterOperationService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private StackInstanceStatusChecker stackInstanceStatusChecker;

    @Mock
    private StackSyncService stackSyncService;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterStatusService clusterStatusService;

    private Stack stack;

    @Mock
    private ClusterStatusResult clusterStatusResult;

    private User user;

    private Workspace workspace;

    @Mock
    private InstanceMetaData instanceMetaData;

    @Mock
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;

    @Mock
    private RuntimeVersionService runtimeVersionService;

    @Before
    public void init() {
        Tracer tracer = Mockito.mock(Tracer.class);
        underTest = new StackStatusCheckerJob(tracer);
        MockitoAnnotations.initMocks(this);
        when(flowLogService.isOtherFlowRunning(anyLong())).thenReturn(Boolean.FALSE);
        underTest.setLocalId("1");
        underTest.setRemoteResourceCrn("remote:crn");

        stack = new Stack();
        stack.setId(1L);
        workspace = new Workspace();
        workspace.setId(1L);
        stack.setWorkspace(workspace);
        stack.setCluster(new Cluster());
        user = new User();
        user.setUserId("1");
        stack.setCreator(user);

        when(stackService.get(anyLong())).thenReturn(stack);
    }

    @After
    public void tearDown() {
        validateMockitoUsage();
    }

    @Test
    public void testNotRunningIfFlowInProgress() throws JobExecutionException {
        when(flowLogService.isOtherFlowRunning(anyLong())).thenReturn(Boolean.TRUE);
        underTest.executeTracedJob(jobExecutionContext);

        verify(stackService, times(0)).getByIdWithListsInTransaction(anyLong());
    }

    @Test
    public void testNotRunningIfStackFailedOrBeingDeleted() throws JobExecutionException {
        setStackStatus(DetailedStackStatus.DELETE_COMPLETED);
        underTest.executeTracedJob(jobExecutionContext);

        verify(clusterApiConnectors, times(0)).getConnector(stack);
    }

    @Test
    public void testInstanceSyncIfCMNotAccessible() throws JobExecutionException {
        setupForCMNotAccessible();
        underTest.executeTracedJob(jobExecutionContext);

        verify(stackInstanceStatusChecker).queryInstanceStatuses(eq(stack), any());
    }

    @Test
    public void testInstanceSyncCMNotRunning() throws JobExecutionException {
        setupForCM();
        underTest.executeTracedJob(jobExecutionContext);

        verify(clusterOperationService, times(0)).reportHealthChange(anyString(), any(), anySet());
        verify(stackInstanceStatusChecker).queryInstanceStatuses(eq(stack), any());
    }

    @Test
    public void testInstanceSyncCMRunning() throws JobExecutionException {
        setupForCM();
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        underTest.executeTracedJob(jobExecutionContext);

        verify(clusterOperationService, times(1)).reportHealthChange(any(), any(), anySet());
        verify(stackInstanceStatusChecker).queryInstanceStatuses(eq(stack), any());
        verify(clusterService, times(1)).updateClusterCertExpirationState(stack.getCluster(), true);
    }

    @Test
    public void testHandledAllStatesSeparately() {
        Set<Status> unshedulableStates = underTest.unshedulableStates();
        Set<Status> ignoredStates = underTest.ignoredStates();
        Set<Status> syncableStates = underTest.syncableStates();

        assertTrue(Sets.intersection(unshedulableStates, ignoredStates).isEmpty());
        assertTrue(Sets.intersection(unshedulableStates, syncableStates).isEmpty());
        assertTrue(Sets.intersection(ignoredStates, syncableStates).isEmpty());

        Set<Status> allPossibleStates = EnumSet.allOf(Status.class);
        Set<Status> allHandledStates = EnumSet.copyOf(unshedulableStates);
        allHandledStates.addAll(ignoredStates);
        allHandledStates.addAll(syncableStates);
        assertEquals(allPossibleStates, allHandledStates);
    }

    private void setupForCM() {
        setStackStatus(DetailedStackStatus.AVAILABLE);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        when(clusterStatusService.isClusterManagerRunningQuickCheck()).thenReturn(true);
        Set<HealthCheck> healthChecks = Sets.newHashSet(new HealthCheck(HealthCheckType.HOST, HealthCheckResult.HEALTHY, Optional.empty()),
                new HealthCheck(HealthCheckType.CERT, HealthCheckResult.UNHEALTHY, Optional.empty()));
        ExtendedHostStatuses extendedHostStatuses = new ExtendedHostStatuses(Map.of(HostName.hostName("host1"), healthChecks));
        when(clusterStatusService.getExtendedHostStatuses(any())).thenReturn(extendedHostStatuses);
        when(instanceMetaDataService.findNotTerminatedForStack(anyLong())).thenReturn(Set.of(instanceMetaData));
        when(instanceMetaData.getInstanceStatus()).thenReturn(InstanceStatus.SERVICES_HEALTHY);
    }

    private void setupForCMNotAccessible() {
        setStackStatus(DetailedStackStatus.STOPPED);
        when(instanceMetaDataService.findNotTerminatedForStack(anyLong())).thenReturn(Set.of(instanceMetaData));
    }

    private void setStackStatus(DetailedStackStatus detailedStackStatus) {
        stack.setStackStatus(new StackStatus(stack, detailedStackStatus));
    }
}

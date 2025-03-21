package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.WORKER;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;

public class DistroXScaleEdgeCasesTest extends AbstractE2ETest {

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private DistroxUtil distroxUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createEnvironmentWithNetworkAndFreeIpa(testContext);
        createDatalake(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "a running environment",
            when = "a valid DistroX create request is sent and one random worker node removed from provider",
            then = "lost node should be removable from cluster")
    public void testCreateDistroxAndRemoveLostNode(TestContext testContext) {
        testContext.given(DistroXTestDto.class)
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .then((tc, testDto, client) -> {
                    List<String> instancesToDelete = distroxUtil.getInstanceIds(testDto, client, WORKER.getName()).stream()
                            .limit(1).collect(Collectors.toList());
                    testContext.getCloudProvider().getCloudFunctionality().deleteInstances(testDto.getName(), instancesToDelete);
                    testDto.setRemovableInstanceId(instancesToDelete.iterator().next());
                    return testDto;
                })
                .awaitForRemovableInstance()
                .when(distroXTestClient.removeInstance())
                .await(STACK_AVAILABLE)
                .validate();
    }

}

package com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion;

import static java.lang.String.format;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.PreconditionSdxE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxBackupStatusResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxRestoreStatusResponse;

public class SdxBackupRestoreTest extends PreconditionSdxE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxBackupRestoreTest.class);

    @Inject
    private SdxTestClient sdxTestClient;

    private String backupId;

    private String restoreId;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithNetworkAndFreeIpa(testContext);
        createSdx(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT, description = "We need to wait the CB-13322 to be resolved")
    @UseSpotInstances
    @Description(
            given = "there is a running Manowar SDX cluster in available state",
            when = "a basic SDX backup then restore request has been sent",
            then = "SDX restore should be done successfully"
    )
    public void testSDXBackupRestoreCanBeSuccessful(TestContext testContext) {
        SdxTestDto sdxTestDto = testContext.given(SdxTestDto.class);
        String cloudStorageBaseLocation = sdxTestDto.getResponse().getCloudStorageBaseLocation();
        String backupObject = "backups";
        testContext
                .given(SdxTestDto.class)
                .when(sdxTestClient.sync())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .when(sdxTestClient.backup(StringUtils.join(List.of(cloudStorageBaseLocation, backupObject), "/"), null))
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then(this::validateDatalakeBackupStatus)
                .then(this::validateDatalakeStatus)
                .then((tc, testDto, client) -> {
                    getCloudFunctionality(tc).cloudStorageListContainer(cloudStorageBaseLocation, backupObject, true);
                    return testDto;
                })
                .validate();

        testContext
                .given(SdxTestDto.class)
                .when(sdxTestClient.restore(backupId, null))
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then(this::validateDatalakeRestoreStatus)
                .then(this::validateDatalakeStatus)
                .validate();
    }

    private SdxTestDto validateDatalakeStatus(TestContext testContext, SdxTestDto testDto, SdxClient client) {
        String statuReason = client.getDefaultClient()
                .sdxEndpoint()
                .getDetailByCrn(testDto.getCrn(), Collections.emptySet())
                .getStatusReason();
        if (statuReason.contains("Datalake backup failed")) {
            LOGGER.error(String.format(" Sdx '%s' backup has been failed: '%s' ", testDto.getName(), statuReason));
            throw new TestFailException(String.format(" Sdx '%s' backup has been failed: '%s' ", testDto.getName(), statuReason));
        } else if (statuReason.contains("Datalake restore failed")) {
            LOGGER.error(String.format(" Sdx '%s' restore has been failed: '%s' ", testDto.getName(), statuReason));
            throw new TestFailException(String.format(" Sdx '%s' restore has been failed: '%s' ", testDto.getName(), statuReason));
        } else {
            LOGGER.info(String.format(" Sdx '%s' backup/restore has been done with '%s'. ", testDto.getName(), statuReason));
            Log.then(LOGGER, format(" Sdx '%s' backup/restore has been done with '%s'. ", testDto.getName(), statuReason));
        }
        return testDto;
    }

    private SdxTestDto validateDatalakeBackupStatus(TestContext testContext, SdxTestDto testDto, SdxClient client) {
        String sdxName = testDto.getName();
        backupId = client.getDefaultClient()
                .sdxBackupEndpoint()
                .getDatalakeBackupId(sdxName, null);
        SdxBackupStatusResponse sdxBackupStatusResponse = client.getDefaultClient()
                .sdxBackupEndpoint()
                .getBackupDatalakeStatus(sdxName, backupId, null);
        String status = sdxBackupStatusResponse.getStatus();
        String statusReason = sdxBackupStatusResponse.getReason();
        LOGGER.info(format(" SDX '%s' backup '%s' status '%s', because of %s ", sdxName, backupId, status, statusReason));
        if (status.contains("FAILED")) {
            LOGGER.error(String.format(" Sdx '%s' backup has been failed: '%s' ", testDto.getName(), statusReason));
            throw new TestFailException(String.format(" Sdx '%s' backup has been failed: '%s' ", testDto.getName(), statusReason));
        } else {
            LOGGER.info(String.format(" Sdx '%s' backup has been done with '%s'. ", testDto.getName(), statusReason));
            Log.then(LOGGER, format(" Sdx '%s' backup has been done with '%s'. ", testDto.getName(), statusReason));
        }
        return testDto;
    }

    private SdxTestDto validateDatalakeRestoreStatus(TestContext testContext, SdxTestDto testDto, SdxClient client) {
        String sdxName = testDto.getName();
        String status;
        String statusReason;

        try {
            restoreId = client.getDefaultClient()
                    .sdxRestoreEndpoint()
                    .getDatalakeRestoreId(sdxName, null);
            SdxRestoreStatusResponse sdxRestoreStatusResponse = client.getDefaultClient()
                    .sdxRestoreEndpoint()
                    .getRestoreDatalakeStatus(sdxName, restoreId, null);
            status = sdxRestoreStatusResponse.getStatus();
            statusReason = sdxRestoreStatusResponse.getReason();
            LOGGER.info(format(" SDX '%s' restore '%s' status '%s', because of %s ", sdxName, restoreId, status, statusReason));
        } catch (Exception e) {
            if (e.getMessage().contains("not found")) {
                throw new TestFailException(String.format(" NOT FOUND :: Cannot get status information for restore '%s' on datalake '%s'." +
                        " Please check the selected backup was successful and the related backup ID is correct. ",
                        restoreId, testDto.getName()), e.getCause());
            }
            throw e;
        }

        if (StringUtils.isBlank(status)) {
            LOGGER.error(String.format(" Sdx '%s' restore status is not available ", testDto.getName()));
            throw new TestFailException(String.format(" Sdx '%s' restore status is not available  ", testDto.getName()));
        } else if (status.contains("FAILED")) {
            LOGGER.error(String.format(" Sdx '%s' restore has been failed: '%s' ", testDto.getName(), statusReason));
            throw new TestFailException(String.format(" Sdx '%s' restore has been failed: '%s' ", testDto.getName(), statusReason));
        } else {
            LOGGER.info(String.format(" Sdx '%s' restore has been done with '%s'. ", testDto.getName(), statusReason));
            Log.then(LOGGER, format(" Sdx '%s' restore has been done with '%s'. ", testDto.getName(), statusReason));
        }
        return testDto;
    }
}

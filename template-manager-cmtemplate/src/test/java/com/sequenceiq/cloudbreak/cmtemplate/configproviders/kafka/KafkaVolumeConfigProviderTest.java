package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaRoles.KAFKA_BROKER;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaRoles.KAFKA_SERVICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cmtemplate.configproviders.VolumeConfigProviderTestHelper;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@ExtendWith(MockitoExtension.class)
class KafkaVolumeConfigProviderTest {

    private final KafkaVolumeConfigProvider provider = new KafkaVolumeConfigProvider();

    @Test
    void getKafkaVolumeConfigMinimum() {
        assertEquals(List.of(config("log.dirs", "/hadoopfs/fs1/kafka")),
                provider.getRoleConfigs(KAFKA_BROKER, null, source(4, 1))
        );
    }

    @Test
    void getKafkaVolumeConfigWithEqualVolume() {
        assertEquals(List.of(config("log.dirs", "/hadoopfs/fs1/kafka,/hadoopfs/fs2/kafka,/hadoopfs/fs3/kafka")),
                provider.getRoleConfigs(KAFKA_BROKER, null, source(3, 3))
        );
    }

    @Test
    void getKafkaVolumeConfigWithZeroVolume() {
        assertEquals(List.of(config("log.dirs", "/hadoopfs/root1/kafka")),
                provider.getRoleConfigs(KAFKA_BROKER, null, source(0, 0))
        );
    }

    @Test
    void getKafkaVolumeConfigWithIncorrectRoleType() {
        assertEquals(List.of(),
                provider.getRoleConfigs(KAFKA_SERVICE, null, source(5, 2))
        );
    }

    private TemplatePreparationObject source(int brokerVolumeCount, int coreBrokerVolumeCount) {
        HostgroupView broker = VolumeConfigProviderTestHelper.hostGroupWithVolumeCount(brokerVolumeCount);
        HostgroupView coreBroker = VolumeConfigProviderTestHelper.hostGroupWithVolumeCount(coreBrokerVolumeCount);
        TemplatePreparationObject source = mock(TemplatePreparationObject.class);
        when(source.getHostGroupsWithComponent(KAFKA_BROKER)).thenReturn(Stream.of(broker, coreBroker));

        return source;
    }
}

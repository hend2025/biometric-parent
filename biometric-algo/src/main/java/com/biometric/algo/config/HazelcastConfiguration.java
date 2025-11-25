package com.biometric.algo.config;

import com.hazelcast.config.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastConfiguration {
    public static final String FACE_FEATURE_MAP = "face-features";

    @Value("${hazelcast.cluster.name:biometric-cluster}")
    private String clusterName;

    @Value("${hazelcast.cluster.members:127.0.0.1}")
    private String members;

    @Bean
    public Config hazelcastConfig() {

        Config config = new Config();
        config.setClusterName(clusterName);

        int cpuCores = Runtime.getRuntime().availableProcessors();
        config.setProperty("hazelcast.operation.thread.count", String.valueOf(Math.max(cpuCores, 16)));
        config.setProperty("hazelcast.operation.generic.thread.count", String.valueOf(Math.max(cpuCores / 2, 8)));
        config.setProperty("hazelcast.io.thread.count", String.valueOf(Math.max(cpuCores / 2, 8)));
        config.setProperty("hazelcast.partition.count", "271");
        config.setProperty("hazelcast.query.result.size.limit", "-1");
        config.setProperty("hazelcast.query.max.local.partition.limit.for.precheck", "3");

        config.setProperty("hazelcast.aggregation.accumulation.parallel.evaluation", "true");
        config.setProperty("hazelcast.partition.operation.thread.count", String.valueOf(Math.max(cpuCores, 16)));
        config.setProperty("hazelcast.slow.operation.detector.threshold.millis", "10000");

        NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig.setPort(5701);
        networkConfig.setPortAutoIncrement(true);

        JoinConfig joinConfig = networkConfig.getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);

        TcpIpConfig tcpIpConfig = joinConfig.getTcpIpConfig();
        tcpIpConfig.setEnabled(true);
        String[] memberList = members.split(",");
        for (String member : memberList) {
            tcpIpConfig.addMember(member.trim());
        }

        MapConfig mapConfig = new MapConfig();
        mapConfig.setName(FACE_FEATURE_MAP);
        mapConfig.setBackupCount(0);
        mapConfig.setAsyncBackupCount(0);
        mapConfig.setReadBackupData(true);

        // 模式会导致堆内存中存在数百万个 Java 小对象，引发频繁 Full GC
        mapConfig.setInMemoryFormat(InMemoryFormat.BINARY);

        mapConfig.setStatisticsEnabled(true);
        mapConfig.setPerEntryStatsEnabled(false);

        IndexConfig groupIndex = new IndexConfig(IndexType.HASH, "groupIds[any]");
        groupIndex.setName("idx_group_ids");
        mapConfig.addIndexConfig(groupIndex);

        config.addMapConfig(mapConfig);

        // 优化序列化配置
        config.getSerializationConfig()
                .setAllowUnsafe(true)
                .setUseNativeByteOrder(true)
                .setEnableCompression(false)
                .setPortableVersion(1);

        return config;

    }

}
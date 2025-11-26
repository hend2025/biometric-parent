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

    // 是否启用数据备份（生产环境建议启用）
    @Value("${hazelcast.backup.enabled:false}")
    private boolean backupEnabled;

    // 备份副本数（建议1-2）
    @Value("${hazelcast.backup.count:1}")
    private int backupCount;

    @Bean
    public Config hazelcastConfig() {

        Config config = new Config();
        config.setClusterName(clusterName);

        int cpuCores = Runtime.getRuntime().availableProcessors();

        String[] memberList = members.split(",");
        int expectedNodeCount = memberList.length;

        // 质数选择：271(默认), 541, 1009, 2003, 4001, 5003
        int partitionCount = calculateOptimalPartitionCount(expectedNodeCount, cpuCores);
        config.setProperty("hazelcast.partition.count", String.valueOf(partitionCount));

        config.setProperty("hazelcast.operation.thread.count", String.valueOf(Math.max(cpuCores, 16)));
        config.setProperty("hazelcast.operation.generic.thread.count", String.valueOf(Math.max(cpuCores / 2, 8)));
        config.setProperty("hazelcast.io.thread.count", String.valueOf(Math.max(cpuCores / 2, 8)));
        config.setProperty("hazelcast.query.result.size.limit", "-1");
        config.setProperty("hazelcast.query.max.local.partition.limit.for.precheck", "3");

        config.setProperty("hazelcast.aggregation.accumulation.parallel.evaluation", "true");
        config.setProperty("hazelcast.partition.operation.thread.count", String.valueOf(Math.max(cpuCores, 16)));
        config.setProperty("hazelcast.slow.operation.detector.threshold.millis", "10000");

        // ========================================
        // 集群性能优化
        // ========================================
        // 心跳检测间隔（多节点建议5秒）
        config.setProperty("hazelcast.heartbeat.interval.seconds", "5");
        // 最大心跳超时（建议60秒，避免网络抖动误判）
        config.setProperty("hazelcast.max.no.heartbeat.seconds", "60");
        // 合并首次运行延迟（集群启动后等待时间）
        config.setProperty("hazelcast.merge.first.run.delay.seconds", "300");
        // 合并下次运行延迟
        config.setProperty("hazelcast.merge.next.run.delay.seconds", "120");
        // 启用操作调用统计（生产环境可关闭以提升性能）
        config.setProperty("hazelcast.operation.call.timeout.millis", "60000");
        // 分区迁移超时（大数据量建议增加）
        config.setProperty("hazelcast.partition.migration.timeout", "300");


        NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig.setPort(5701);
        networkConfig.setPortAutoIncrement(true);

        JoinConfig joinConfig = networkConfig.getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);

        TcpIpConfig tcpIpConfig = joinConfig.getTcpIpConfig();
        tcpIpConfig.setEnabled(true);
        for (String member : memberList) {
            tcpIpConfig.addMember(member.trim());
        }

        MapConfig mapConfig = new MapConfig();
        mapConfig.setName(FACE_FEATURE_MAP);
        mapConfig.setBackupCount(0);
        mapConfig.setAsyncBackupCount(0);
        // 允许从备份读取，提升读性能
        mapConfig.setReadBackupData(true);

        if (backupEnabled && expectedNodeCount > 1) {
            mapConfig.setBackupCount(Math.min(backupCount, expectedNodeCount - 1));
            mapConfig.setAsyncBackupCount(expectedNodeCount >= 4 ? 1 : 0);
        } else {
            mapConfig.setBackupCount(0);
            mapConfig.setAsyncBackupCount(0);
        }

        // 关键点：使用 BINARY 格式配合 Hazelcast 序列化效果最好
        mapConfig.setInMemoryFormat(InMemoryFormat.BINARY);

        // 驱逐策略（可选，防止内存溢出）
        // mapConfig.setEvictionConfig(new EvictionConfig()
        //     .setEvictionPolicy(EvictionPolicy.LRU)
        //     .setMaxSizePolicy(MaxSizePolicy.USED_HEAP_PERCENTAGE)
        //     .setSize(80)); // 堆内存使用超过80%时驱逐

        mapConfig.setStatisticsEnabled(true);
        mapConfig.setPerEntryStatsEnabled(false);

        IndexConfig groupIndex = new IndexConfig(IndexType.HASH, "groupIds[any]");
        groupIndex.setName("idx_group_ids");
        mapConfig.addIndexConfig(groupIndex);

        config.addMapConfig(mapConfig);

        // --- 注册自定义序列化工厂 ---
        config.getSerializationConfig()
                .addDataSerializableFactory(
                        BiometricDataSerializableFactory.FACTORY_ID,
                        new BiometricDataSerializableFactory()
                )
                .setAllowUnsafe(true)
                .setUseNativeByteOrder(true)
                .setEnableCompression(false)
                .setPortableVersion(1);

        return config;

    }

    /**
     * 计算最优分区数（必须是质数）
     * 公式：partitionCount ≈ 节点数 × CPU核心数 × 倍数(7-10)
     *
     * @param nodeCount 集群节点数
     * @param cpuCores CPU核心数
     * @return 最优分区数（质数）
     */
    private int calculateOptimalPartitionCount(int nodeCount, int cpuCores) {
        // 推荐的质数列表
        int[] primes = {271, 541, 1009, 2003, 4001, 5003, 10007};

        // 计算目标值（使用倍数8作为平衡点）
        int target = nodeCount * cpuCores * 8;

        // 查找最接近的质数
        int bestPrime = primes[0];
        int minDiff = Math.abs(target - bestPrime);

        for (int prime : primes) {
            int diff = Math.abs(target - prime);
            if (diff < minDiff) {
                minDiff = diff;
                bestPrime = prime;
            }
        }

        return bestPrime;
    }

}
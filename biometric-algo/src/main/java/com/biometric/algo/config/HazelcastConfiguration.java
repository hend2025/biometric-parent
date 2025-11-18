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

        config.setProperty("hazelcast.operation.thread.count", "8");
        config.setProperty("hazelcast.operation.generic.thread.count", "4");
        config.setProperty("hazelcast.io.thread.count", "4");
        config.setProperty("hazelcast.partition.count", "271");

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
        
        mapConfig.setInMemoryFormat(InMemoryFormat.BINARY);
        
        mapConfig.setStatisticsEnabled(false);
        
        NearCacheConfig nearCacheConfig = new NearCacheConfig();
        nearCacheConfig.setInMemoryFormat(InMemoryFormat.BINARY);
        nearCacheConfig.setInvalidateOnChange(true);
        nearCacheConfig.setTimeToLiveSeconds(300);
        nearCacheConfig.setMaxIdleSeconds(180);
        nearCacheConfig.setCacheLocalEntries(false);
        
        EvictionConfig evictionConfig = new EvictionConfig();
        evictionConfig.setEvictionPolicy(EvictionPolicy.LRU);
        evictionConfig.setMaxSizePolicy(MaxSizePolicy.ENTRY_COUNT);
        evictionConfig.setSize(50000);
        nearCacheConfig.setEvictionConfig(evictionConfig);
        
        mapConfig.setNearCacheConfig(nearCacheConfig);

        IndexConfig groupIndex = new IndexConfig(IndexType.HASH, "groupIds[any]");
        mapConfig.addIndexConfig(groupIndex);

        config.addMapConfig(mapConfig);

        return config;

    }

}
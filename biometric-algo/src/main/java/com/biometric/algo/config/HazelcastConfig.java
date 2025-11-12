package com.biometric.algo.config;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastConfig {

    @Value("${hazelcast.cluster.name:biometric-cluster}")
    private String clusterName;

    @Value("${hazelcast.port:5701}")
    private int port;

    @Value("${hazelcast.members:127.0.0.1}")
    private String members;

    @Bean
    public HazelcastInstance hazelcastInstance() {
        Config config = new Config();
        config.setClusterName(clusterName);

        NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig.setPortAutoIncrement(true);
        networkConfig.setPortCount(100);

        JoinConfig joinConfig = networkConfig.getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);

        TcpIpConfig tcpIpConfig = joinConfig.getTcpIpConfig();
        tcpIpConfig.setEnabled(true);
        String[] memberList = members.split(",");
        for (String member : memberList) {
            tcpIpConfig.addMember(member.trim());
        }

        MapConfig faceFeatureMapConfig = new MapConfig();
        faceFeatureMapConfig.setName("faceFeatureMap");
        faceFeatureMapConfig.setInMemoryFormat(com.hazelcast.config.InMemoryFormat.BINARY);
        faceFeatureMapConfig.setBackupCount(1);
        faceFeatureMapConfig.setAsyncBackupCount(1);
        faceFeatureMapConfig.setTimeToLiveSeconds(0);
        faceFeatureMapConfig.setMaxIdleSeconds(0);
        
        EvictionConfig evictionConfig = new EvictionConfig();
        evictionConfig.setEvictionPolicy(EvictionPolicy.LRU);
        evictionConfig.setMaxSizePolicy(MaxSizePolicy.PER_NODE);
        evictionConfig.setSize(10000000);
        faceFeatureMapConfig.setEvictionConfig(evictionConfig);
        
        config.addMapConfig(faceFeatureMapConfig);

        return Hazelcast.newHazelcastInstance(config);
    }

}

package com.biometric.serv.listener;

import com.biometric.serv.service.DataLoadService;
import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 应用启动监听器，负责在多节点环境下自动加载数据到Hazelcast
 * 每个节点根据其在集群中的索引加载对应分片的数据
 */
@Component
public class DataLoadStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(DataLoadStartupListener.class);

    @Value("${hazelcast.cluster.members:127.0.0.1}")
    private String members;

    @Autowired
    private DataLoadService dataLoadService;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 异步加载数据，避免阻塞应用启动
        CompletableFuture.runAsync(() -> {
            try {
                // 等待Hazelcast集群稳定并达到预期节点数
                waitForClusterReady();
                
                loadDataForCurrentNode();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("数据加载被中断", e);
            } catch (Exception e) {
                log.error("数据加载失败", e);
            }
        });
    }

    /**
     * 为当前节点加载对应分片的数据
     */
    private void loadDataForCurrentNode() {
        try {
            // 获取节点索引和总节点数
            NodeInfo nodeInfo = determineNodeInfo();
            
            log.info("开始为节点 {}/{} 加载数据到Hazelcast", nodeInfo.nodeIndex, nodeInfo.totalNodes);
            log.info("当前节点地址: {}", nodeInfo.currentMemberAddress);
            log.info("集群成员列表: {}", nodeInfo.allMemberAddresses);

            // 加载当前节点负责的数据分片
            dataLoadService.loadAllFeaturesIntoCache(nodeInfo.nodeIndex, nodeInfo.totalNodes);

            log.info("节点 {}/{} 数据加载完成", nodeInfo.nodeIndex, nodeInfo.totalNodes);

        } catch (Exception e) {
            log.error("节点数据加载失败", e);
            throw new RuntimeException("Failed to load data for current node", e);
        }
    }

    /**
     * 等待Hazelcast集群达到预期节点数
     * 如果配置了expectedNodes，会等待直到集群成员数达到预期值
     * 否则只等待固定时间让集群稳定
     */
    private void waitForClusterReady() throws InterruptedException {
        Integer expectedNodes = members.split(",").length;
        if (expectedNodes == null || expectedNodes <= 0) {
            // 未配置预期节点数，只等待固定时间
            log.info("未配置预期节点数，等待10秒让Hazelcast集群稳定...");
            Thread.sleep(10000);
            return;
        }
        
        // 配置了预期节点数，等待直到达到预期数量
        log.info("等待Hazelcast集群达到预期节点数: {}", expectedNodes);
        int maxWaitSeconds = 30;
        int waitedSeconds = 0;
        
        while (waitedSeconds < maxWaitSeconds) {
            int currentSize = hazelcastInstance.getCluster().getMembers().size();
            
            if (currentSize >= expectedNodes) {
                log.info("集群已达到预期节点数: {}/{}", currentSize, expectedNodes);
                // 再等待2秒确保集群完全稳定
                Thread.sleep(2000);
                return;
            }
            
            if (waitedSeconds % 10 == 0) {
                log.info("当前集群节点数: {}/{}，继续等待...", currentSize, expectedNodes);
            }
            
            Thread.sleep(1000);
            waitedSeconds++;
        }
        
        int finalSize = hazelcastInstance.getCluster().getMembers().size();
        log.warn("等待超时！当前集群节点数: {}/{}，将继续执行数据加载", finalSize, expectedNodes);
    }

    /**
     * 确定当前节点的索引和集群总节点数
     */
    private NodeInfo determineNodeInfo() {
        NodeInfo info = new NodeInfo();
        try {
            // 自动从Hazelcast集群中获取节点信息
            Set<Member> members = hazelcastInstance.getCluster().getMembers();
            Member localMember = hazelcastInstance.getCluster().getLocalMember();

            // 将成员按地址排序，确保所有节点的顺序一致
            List<Member> sortedMembers = new ArrayList<>(members);
            sortedMembers.sort(Comparator.comparing(m -> m.getAddress().toString()));

            info.totalNodes = sortedMembers.size();
            info.nodeIndex = sortedMembers.indexOf(localMember);
            info.currentMemberAddress = localMember.getAddress().toString();
            info.allMemberAddresses = new ArrayList<>();
            for (Member member : sortedMembers) {
                info.allMemberAddresses.add(member.getAddress().toString());
            }

            if (info.nodeIndex < 0) {
                throw new IllegalStateException("无法在集群成员列表中找到当前节点");
            }

            log.info("从Hazelcast集群自动获取节点信息: nodeIndex={}, totalNodes={}", info.nodeIndex, info.totalNodes);
            
            return info;

        } catch (Exception e) {
            log.error("无法从Hazelcast集群获取节点信息，使用默认值", e);
            // 降级方案：单节点模式
            info.nodeIndex = 0;
            info.totalNodes = 1;
            info.currentMemberAddress = "unknown";
            info.allMemberAddresses = Collections.singletonList("unknown");
            return info;
        }
    }

    /**
     * 节点信息
     */
    private static class NodeInfo {
        int nodeIndex;
        int totalNodes;
        String currentMemberAddress;
        List<String> allMemberAddresses;
    }

}

package com.biometric.serv.controller;

import com.biometric.algo.service.FaceCacheService;
import com.biometric.serv.service.DataLoadService;
import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 数据加载控制器，提供手动触发数据加载的接口
 */
@RestController
@RequestMapping("/api/data-load")
public class DataLoadController {

    private static final Logger log = LoggerFactory.getLogger(DataLoadController.class);

    @Autowired
    private DataLoadService dataLoadService;

    @Autowired
    private FaceCacheService faceCacheService;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    /**
     * 手动触发当前节点加载数据
     * 
     * @param shardIndex 分片索引（可选，如果不指定则自动从集群中获取）
     * @param totalShards 总分片数（可选，如果不指定则自动从集群中获取）
     * @return 加载结果
     */
    @PostMapping("/load")
    public Map<String, Object> loadData(
            @RequestParam(required = false) Integer shardIndex,
            @RequestParam(required = false) Integer totalShards) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 确定分片参数
            if (shardIndex == null || totalShards == null) {
                NodeInfo nodeInfo = getNodeInfo();
                shardIndex = nodeInfo.nodeIndex;
                totalShards = nodeInfo.totalNodes;
            }

            log.info("手动触发数据加载: shardIndex={}, totalShards={}", shardIndex, totalShards);
            
            // 异步加载
            final int finalShardIndex = shardIndex;
            final int finalTotalShards = totalShards;
            
            CompletableFuture.runAsync(() -> {
                dataLoadService.loadAllFeaturesIntoCache(finalShardIndex, finalTotalShards);
            });

            result.put("success", true);
            result.put("message", "数据加载已启动");
            result.put("shardIndex", shardIndex);
            result.put("totalShards", totalShards);
            
        } catch (Exception e) {
            log.error("手动加载数据失败", e);
            result.put("success", false);
            result.put("message", "数据加载失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 清空缓存
     * 
     * @return 清空结果
     */
    @PostMapping("/clear")
    public Map<String, Object> clearCache() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.warn("手动触发清空缓存");
            faceCacheService.clearCache();
            
            result.put("success", true);
            result.put("message", "缓存已清空");
            
        } catch (Exception e) {
            log.error("清空缓存失败", e);
            result.put("success", false);
            result.put("message", "清空缓存失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取缓存统计信息
     * 
     * @return 统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> getCacheStats() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            NodeInfo nodeInfo = getNodeInfo();
            
            result.put("success", true);
            result.put("nodeIndex", nodeInfo.nodeIndex);
            result.put("totalNodes", nodeInfo.totalNodes);
            result.put("currentMember", nodeInfo.currentMemberAddress);
            result.put("allMembers", nodeInfo.allMemberAddresses);
            result.put("cacheSize", faceCacheService.getFaceFeatureMap().size());
            result.put("localCacheSize", faceCacheService.getFaceFeatureMap().localKeySet().size());
            
        } catch (Exception e) {
            log.error("获取缓存统计失败", e);
            result.put("success", false);
            result.put("message", "获取统计失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取集群信息
     * 
     * @return 集群信息
     */
    @GetMapping("/cluster-info")
    public Map<String, Object> getClusterInfo() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            NodeInfo nodeInfo = getNodeInfo();
            
            result.put("success", true);
            result.put("nodeIndex", nodeInfo.nodeIndex);
            result.put("totalNodes", nodeInfo.totalNodes);
            result.put("currentMember", nodeInfo.currentMemberAddress);
            result.put("allMembers", nodeInfo.allMemberAddresses);
            result.put("clusterName", hazelcastInstance.getConfig().getClusterName());

        } catch (Exception e) {
            log.error("获取集群信息失败", e);
            result.put("success", false);
            result.put("message", "获取集群信息失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取当前节点信息
     */
    private NodeInfo getNodeInfo() {
        NodeInfo info = new NodeInfo();

        // 从Hazelcast集群中获取
        Set<Member> members = hazelcastInstance.getCluster().getMembers();
        Member localMember = hazelcastInstance.getCluster().getLocalMember();

        List<Member> sortedMembers = new ArrayList<>(members);
        sortedMembers.sort(Comparator.comparing(m -> m.getAddress().toString()));

        info.totalNodes = sortedMembers.size();
        info.nodeIndex = sortedMembers.indexOf(localMember);
        info.currentMemberAddress = localMember.getAddress().toString();
        info.allMemberAddresses = new ArrayList<>();
        for (Member member : sortedMembers) {
            info.allMemberAddresses.add(member.getAddress().toString());
        }

        return info;
    }

    private static class NodeInfo {
        int nodeIndex;
        int totalNodes;
        String currentMemberAddress;
        List<String> allMemberAddresses;
    }

}

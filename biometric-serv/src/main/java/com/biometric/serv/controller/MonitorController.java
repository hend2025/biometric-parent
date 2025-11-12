package com.biometric.serv.controller;

import com.biometric.serv.service.PerformanceMonitorService;
import com.biometric.serv.vo.ResultVO;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    @Autowired
    private PerformanceMonitorService performanceMonitorService;

    @Autowired
    @Qualifier("hazelcastInstance")
    private HazelcastInstance hazelcastInstance;

    @GetMapping("/health")
    public ResultVO<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            health.put("status", "UP");
            health.put("timestamp", System.currentTimeMillis());
            
            boolean hazelcastHealthy = hazelcastInstance != null && 
                    hazelcastInstance.getLifecycleService().isRunning();
            health.put("hazelcast", hazelcastHealthy ? "UP" : "DOWN");
            
            if (hazelcastHealthy) {
                health.put("clusterSize", hazelcastInstance.getCluster().getMembers().size());
                IMap<String, ?> faceFeatureMap = hazelcastInstance.getMap("faceFeatureMap");
                health.put("cachedFaceCount", faceFeatureMap.size());
            }
            
            return ResultVO.success("健康检查通过", health);
        } catch (Exception e) {
            log.error("健康检查失败", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResultVO.error("健康检查失败");
        }
    }

    @GetMapping("/metrics")
    public ResultVO<PerformanceMonitorService.PerformanceMetrics> getMetrics() {
        try {
            PerformanceMonitorService.PerformanceMetrics metrics = 
                    performanceMonitorService.getMetrics();
            return ResultVO.success("获取性能指标成功", metrics);
        } catch (Exception e) {
            log.error("获取性能指标失败", e);
            return ResultVO.error("获取性能指标失败");
        }
    }

    @GetMapping("/cluster")
    public ResultVO<Map<String, Object>> getClusterInfo() {
        try {
            Map<String, Object> clusterInfo = new HashMap<>();
            
            if (hazelcastInstance != null) {
                clusterInfo.put("clusterName", hazelcastInstance.getConfig().getClusterName());
                clusterInfo.put("memberCount", hazelcastInstance.getCluster().getMembers().size());
                clusterInfo.put("localMember", hazelcastInstance.getCluster().getLocalMember().getAddress().toString());
                
                java.util.List<String> members = new java.util.ArrayList<>();
                hazelcastInstance.getCluster().getMembers().forEach(member -> 
                    members.add(member.getAddress().toString())
                );
                clusterInfo.put("members", members);
                
                IMap<String, ?> faceFeatureMap = hazelcastInstance.getMap("faceFeatureMap");
                clusterInfo.put("localMapSize", faceFeatureMap.getLocalMapStats().getOwnedEntryCount());
                clusterInfo.put("totalMapSize", faceFeatureMap.size());
            }
            
            return ResultVO.success("获取集群信息成功", clusterInfo);
        } catch (Exception e) {
            log.error("获取集群信息失败", e);
            return ResultVO.error("获取集群信息失败");
        }
    }
}


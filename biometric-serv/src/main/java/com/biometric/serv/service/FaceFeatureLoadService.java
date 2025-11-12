package com.biometric.serv.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.biometric.algo.service.FaceRecognitionService;
import com.biometric.serv.entity.BosgFaceFturD;
import com.biometric.serv.mapper.BosgFaceFturDMapper;
import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FaceFeatureLoadService {

    @Autowired
    private BosgFaceFturDMapper bosgFaceFturDMapper;

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    @Autowired
    @Qualifier("hazelcastInstance")
    private HazelcastInstance hazelcastInstance;

    @Value("${hazelcast.members:}")
    private String configuredMembers;

    @Value("${biometric.face.partition:true}")
    private boolean enablePartition;

    @Value("${biometric.face.load.batchSize:500}")
    private int batchSize;

    @Value("${biometric.face.load.parallelThreads:4}")
    private int parallelThreads;

    private Integer dynamicNodeId = null;
    private Integer dynamicTotalNodes = null;

    public void loadFaceFeaturesToHazelcast() {
        log.info("========== 开始加载人脸特征数据到 Hazelcast ==========");
        
        initializeNodeInfo();
        
        int nodeId = dynamicNodeId != null ? dynamicNodeId : 0;
        int totalNodes = dynamicTotalNodes != null ? dynamicTotalNodes : 1;
        
        log.info("节点配置: 当前节点ID={}, 总节点数={}, 分区加载={} (自动检测)", 
                nodeId, totalNodes, enablePartition);
        
        try {
            QueryWrapper<BosgFaceFturD> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("VALI_FLAG", "1")
                       .eq("FACE_TMPL_STAS", "1")
                       .isNotNull("FACE_FTUR_DATA");
            
            List<BosgFaceFturD> faceFeatures = bosgFaceFturDMapper.selectList(queryWrapper);
            
            if (faceFeatures == null || faceFeatures.isEmpty()) {
                log.warn("没有找到需要加载的人脸特征数据");
                return;
            }
            
            log.info("数据库查询到 {} 条人脸特征数据", faceFeatures.size());
            
            int totalCount = faceFeatures.size();
            List<BosgFaceFturD> nodeFaceFeatures = new ArrayList<>();
            
            for (BosgFaceFturD faceFeature : faceFeatures) {
                if (!enablePartition || isDataBelongsToCurrentNode(faceFeature.getFaceBosgId())) {
                    nodeFaceFeatures.add(faceFeature);
                }
            }
            
            int skippedCount = totalCount - nodeFaceFeatures.size();
            log.info("当前节点需要处理 {} 条数据，跳过 {} 条", nodeFaceFeatures.size(), skippedCount);
            
            int successCount = 0;
            int failCount = 0;
            List<String> invalidLengthList = new ArrayList<>();
            
            int totalBatches = (nodeFaceFeatures.size() + batchSize - 1) / batchSize;
            log.info("开始批量加载，共 {} 批，每批 {} 条", totalBatches, batchSize);
            
            for (int i = 0; i < nodeFaceFeatures.size(); i += batchSize) {
                int end = Math.min(i + batchSize, nodeFaceFeatures.size());
                List<BosgFaceFturD> batch = nodeFaceFeatures.subList(i, end);
                
                int batchNumber = (i / batchSize) + 1;
                log.info("处理第 {}/{} 批数据，共 {} 条", batchNumber, totalBatches, batch.size());
                
                for (BosgFaceFturD faceFeature : batch) {
                    try {
                        byte[] featureVector = faceFeature.getFaceFturData();
                        int len = featureVector == null ? 0 : featureVector.length;
                        if (featureVector == null || len != 512) {
                            invalidLengthList.add(faceFeature.getFaceBosgId());
                            failCount++;
                            continue;
                        }
                        
                        boolean success = addFaceFeatureToAlgoService(
                            faceFeature.getFaceBosgId(),
                            faceFeature.getPsnTmplNo(),
                            featureVector,
                            faceFeature.getFaceImgUrl()
                        );
                        
                        if (success) {
                            successCount++;
                        } else {
                            failCount++;
                        }
                        
                    } catch (Exception e) {
                        failCount++;
                        log.error("处理人脸特征数据异常: faceBosgId={}", 
                                faceFeature.getFaceBosgId(), e);
                    }
                }
                
                if (batchNumber % 10 == 0) {
                    log.info("已完成 {} 批，成功: {}, 失败: {}", batchNumber, successCount, failCount);
                }
            }
            
            if (!invalidLengthList.isEmpty()) {
                log.warn("人脸特征数据长度不是 512 字节的记录数: {}", invalidLengthList.size());
            }
            
            log.info("========== 人脸特征数据加载完成 ==========");
            log.info("数据库总记录数: {}", totalCount);
            if (enablePartition) {
                log.info("分区加载 - 当前节点处理: {}, 跳过(由其他节点处理): {}", 
                        (totalCount - skippedCount), skippedCount);
            }
            log.info("加载结果 - 成功: {}, 失败: {}", successCount, failCount);
            
        } catch (Exception e) {
            log.error("加载人脸特征数据到 Hazelcast 失败", e);
        }
    }

    private void initializeNodeInfo() {
        try {
            if (hazelcastInstance == null) {
                log.warn("HazelcastInstance 未初始化，使用默认节点配置");
                dynamicNodeId = 0;
                dynamicTotalNodes = 1;
                return;
            }

            Set<Member> members = hazelcastInstance.getCluster().getMembers();
            
            if (members == null || members.isEmpty()) {
                log.warn("集群成员为空，使用默认节点配置");
                dynamicNodeId = 0;
                dynamicTotalNodes = 1;
                return;
            }

            Member localMember = hazelcastInstance.getCluster().getLocalMember();
            String localAddress = localMember.getAddress().getHost() + ":" + localMember.getAddress().getPort();
            
            List<Member> sortedMembers = members.stream()
                    .sorted(Comparator.comparing(m -> m.getAddress().getHost() + ":" + m.getAddress().getPort()))
                    .collect(Collectors.toList());

            int configuredMemberCount = getConfiguredMemberCount();
            if (configuredMemberCount > 0) {
                dynamicTotalNodes = configuredMemberCount;
                log.info("使用配置文件的成员数量: {} (来自 hazelcast.members)", dynamicTotalNodes);
            } else {
                dynamicTotalNodes = sortedMembers.size();
                log.info("自动检测到集群成员数量: {}", dynamicTotalNodes);
            }

            dynamicNodeId = 0;
            for (int i = 0; i < sortedMembers.size(); i++) {
                Member member = sortedMembers.get(i);
                String memberAddress = member.getAddress().getHost() + ":" + member.getAddress().getPort();
                
                if (memberAddress.equals(localAddress)) {
                    dynamicNodeId = i;
                    break;
                }
            }

            log.info("自动检测节点信息成功:");
            log.info("  - 当前节点地址: {}", localAddress);
            log.info("  - 当前节点ID: {}", dynamicNodeId);
            log.info("  - 集群成员列表:");
            for (int i = 0; i < sortedMembers.size(); i++) {
                Member member = sortedMembers.get(i);
                String memberAddr = member.getAddress().getHost() + ":" + member.getAddress().getPort();
                String marker = memberAddr.equals(localAddress) ? " <- 当前节点" : "";
                log.info("    [{}] {}{}", i, memberAddr, marker);
            }

        } catch (Exception e) {
            log.error("自动检测节点信息失败，使用默认配置", e);
            dynamicNodeId = 0;
            dynamicTotalNodes = 1;
        }
    }

    private int getConfiguredMemberCount() {
        if (configuredMembers == null || configuredMembers.trim().isEmpty()) {
            return 0;
        }
        
        try {
            String[] members = configuredMembers.split(",");
            int count = 0;
            for (String member : members) {
                if (member != null && !member.trim().isEmpty()) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            log.warn("解析 hazelcast.members 配置失败: {}", e.getMessage());
            return 0;
        }
    }

    private boolean isDataBelongsToCurrentNode(String faceBosgId) {
        int nodeId = dynamicNodeId != null ? dynamicNodeId : 0;
        int totalNodes = dynamicTotalNodes != null ? dynamicTotalNodes : 1;
        
        if (totalNodes <= 1) {
            return true;
        }
        
        if (faceBosgId == null || faceBosgId.isEmpty()) {
            log.warn("faceBosgId为空，默认分配给节点0");
            return nodeId == 0;
        }
        
        int hash = Math.abs(faceBosgId.hashCode());
        int targetNodeId = hash % totalNodes;
        
        return targetNodeId == nodeId;
    }

    private boolean addFaceFeatureToAlgoService(String faceId, String psnNo,
                                                byte[] featureVector, String imageUrl) {
        try {
            faceRecognitionService.addFaceFeatureWithId(faceId, psnNo, featureVector, imageUrl);
            return true;
        } catch (Exception e) {
            log.error("添加人脸特征到 Hazelcast 异常: faceId={}, error={}", faceId, e.getMessage());
            return false;
        }
    }

    public long getTotalFaceFeatureCount() {
        QueryWrapper<BosgFaceFturD> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("VALI_FLAG", "1")
                   .eq("FACE_TMPL_STAS", "1")
                   .isNotNull("FACE_FTUR_DATA");
        
        return bosgFaceFturDMapper.selectCount(queryWrapper);
    }
}

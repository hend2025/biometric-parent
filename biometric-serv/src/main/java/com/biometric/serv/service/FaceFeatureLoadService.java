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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 人脸特征加载服务
 * 用于在服务启动时从数据库加载人脸特征数据到 Hazelcast
 */
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

    // 动态获取的节点信息（在运行时确定）
    private Integer dynamicNodeId = null;
    private Integer dynamicTotalNodes = null;

    /**
     * 加载人脸特征数据到 Hazelcast
     */
    public void loadFaceFeaturesToHazelcast() {
        log.info("========== 开始加载人脸特征数据到 Hazelcast ==========");
        
        // 动态获取节点信息
        initializeNodeInfo();
        
        int nodeId = dynamicNodeId != null ? dynamicNodeId : 0;
        int totalNodes = dynamicTotalNodes != null ? dynamicTotalNodes : 1;
        
        log.info("节点配置: 当前节点ID={}, 总节点数={}, 分区加载={} (自动检测)", 
                nodeId, totalNodes, enablePartition);
        
        try {
            // 查询有效的人脸特征数据
            QueryWrapper<BosgFaceFturD> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("VALI_FLAG", "1")  // 有效标志
                       .eq("FACE_TMPL_STAS", "1")  // 人脸模板状态为有效
                       .isNotNull("FACE_FTUR_DATA");  // 人脸特征数据不为空
            
            List<BosgFaceFturD> faceFeatures = bosgFaceFturDMapper.selectList(queryWrapper);
            
            if (faceFeatures == null || faceFeatures.isEmpty()) {
                log.warn("没有找到需要加载的人脸特征数据");
                return;
            }
            
            log.info("数据库查询到 {} 条人脸特征数据", faceFeatures.size());
            
            int totalCount = 0;
            int skippedCount = 0;
            int successCount = 0;
            int failCount = 0;
            List<String> invalidLengthList = new ArrayList<>();
            
            for (BosgFaceFturD faceFeature : faceFeatures) {
                totalCount++;
                
                try {
                    // 分区逻辑：根据节点ID判断是否由当前节点处理
                    if (enablePartition && !isDataBelongsToCurrentNode(faceFeature.getFaceBosgId())) {
                        skippedCount++;
                        if (skippedCount <= 3) {
                            log.debug("跳过不属于当前节点的数据: faceBosgId={}", faceFeature.getFaceBosgId());
                        }
                        continue;
                    }
                    
                    byte[] featureVector = faceFeature.getFaceFturData();
                    int len = featureVector == null ? 0 : featureVector.length;
                    if (featureVector == null || len != 512) {
                        invalidLengthList.add(faceFeature.getFaceBosgId());
                        log.warn("人脸特征数据长度无效(期望512字节，实际{}字节)，跳过: faceBosgId={}", len, faceFeature.getFaceBosgId());
                        failCount++;
                        continue;
                    }
                    
                    // 调用算法服务添加人脸特征
                    boolean success = addFaceFeatureToAlgoService(
                        faceFeature.getFaceBosgId(),
                        faceFeature.getPsnTmplNo(),
                        featureVector,
                        faceFeature.getFaceImgUrl()
                    );
                    
                    if (success) {
                        successCount++;
                        if (successCount % 100 == 0) {
                            log.info("当前节点已成功加载 {} 条人脸特征数据", successCount);
                        }
                    } else {
                        failCount++;
                        log.warn("加载人脸特征失败: faceBosgId={}", faceFeature.getFaceBosgId());
                    }
                    
                } catch (Exception e) {
                    failCount++;
                    log.error("处理人脸特征数据异常: faceBosgId={}, error={}", 
                            faceFeature.getFaceBosgId(), e.getMessage());
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

    /**
     * 初始化节点信息
     * 从 Hazelcast 集群中自动获取当前节点ID和总节点数
     */
    private void initializeNodeInfo() {
        try {
            if (hazelcastInstance == null) {
                log.warn("HazelcastInstance 未初始化，使用默认节点配置");
                dynamicNodeId = 0;
                dynamicTotalNodes = 1;
                return;
            }

            // 获取集群所有成员
            Set<Member> members = hazelcastInstance.getCluster().getMembers();
            
            if (members == null || members.isEmpty()) {
                log.warn("集群成员为空，使用默认节点配置");
                dynamicNodeId = 0;
                dynamicTotalNodes = 1;
                return;
            }

            // 获取当前成员
            Member localMember = hazelcastInstance.getCluster().getLocalMember();
            String localAddress = localMember.getAddress().getHost() + ":" + localMember.getAddress().getPort();
            
            // 按照成员的地址排序（确保所有节点的排序结果一致）
            List<Member> sortedMembers = members.stream()
                    .sorted(Comparator.comparing(m -> m.getAddress().getHost() + ":" + m.getAddress().getPort()))
                    .collect(Collectors.toList());

            // 确定总节点数
            // 优先使用配置文件中的成员数量
            int configuredMemberCount = getConfiguredMemberCount();
            if (configuredMemberCount > 0) {
                dynamicTotalNodes = configuredMemberCount;
                log.info("使用配置文件的成员数量: {} (来自 hazelcast.members)", dynamicTotalNodes);
            } else {
                // 如果配置为空，使用实际集群成员数量
                dynamicTotalNodes = sortedMembers.size();
                log.info("自动检测到集群成员数量: {}", dynamicTotalNodes);
            }

            // 查找当前节点在排序列表中的位置（即节点ID）
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

    /**
     * 从配置文件中获取成员数量
     * 解析 hazelcast.members 配置，计算节点总数
     */
    private int getConfiguredMemberCount() {
        if (configuredMembers == null || configuredMembers.trim().isEmpty()) {
            return 0;
        }
        
        try {
            // 按逗号分割成员列表
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

    /**
     * 判断数据是否属于当前节点
     * 使用一致性哈希算法，根据 faceBosgId 的哈希值取模分配到不同节点
     * 
     * @param faceBosgId 人脸特征ID
     * @return true=属于当前节点，false=属于其他节点
     */
    private boolean isDataBelongsToCurrentNode(String faceBosgId) {
        int nodeId = dynamicNodeId != null ? dynamicNodeId : 0;
        int totalNodes = dynamicTotalNodes != null ? dynamicTotalNodes : 1;
        
        if (totalNodes <= 1) {
            // 只有一个节点，所有数据都属于当前节点
            return true;
        }
        
        if (faceBosgId == null || faceBosgId.isEmpty()) {
            log.warn("faceBosgId为空，默认分配给节点0");
            return nodeId == 0;
        }
        
        // 使用String的hashCode进行哈希，然后取绝对值避免负数
        int hash = Math.abs(faceBosgId.hashCode());
        int targetNodeId = hash % totalNodes;
        
        return targetNodeId == nodeId;
    }

    /**
     * 将 byte[] 转换为 float[]
     * 人脸特征数据通常是 128 维或 512 维的浮点数数组
     */
    private float[] bytesToFloatArray(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        
        // 4 bytes per float
        if (bytes.length % 4 != 0) {
            log.warn("人脸特征数据长度不是 4 的倍数: {}", bytes.length);
            return null;
        }
        
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);  // 根据实际情况调整字节序
        
        float[] floatArray = new float[bytes.length / 4];
        for (int i = 0; i < floatArray.length; i++) {
            floatArray[i] = buffer.getFloat();
        }
        
        return floatArray;
    }

    /**
     * 添加人脸特征到 Hazelcast
     */
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

    /**
     * 获取数据库中人脸特征的总数
     */
    public long getTotalFaceFeatureCount() {
        QueryWrapper<BosgFaceFturD> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("VALI_FLAG", "1")
                   .eq("FACE_TMPL_STAS", "1")
                   .isNotNull("FACE_FTUR_DATA");
        
        return bosgFaceFturDMapper.selectCount(queryWrapper);
    }
}


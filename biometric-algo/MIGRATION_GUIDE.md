# 迁移指南 - SocketService 重构

## 概述

本文档提供从旧版 `SocketService` 迁移到新版 `SocketServiceRefactored` 的详细指南。

---

## 快速对比

| 特性 | SocketService (旧版) | SocketServiceRefactored (新版) |
|------|---------------------|--------------------------------|
| 代码行数 | 362行 | 257行 |
| 设计模式 | 无 | 8种设计模式 |
| 资源管理 | 手动管理 | try-with-resources |
| 异常处理 | RuntimeException | 自定义异常体系 |
| 代码复用 | 低（重复代码多） | 高（使用策略模式） |
| 可扩展性 | 低 | 高 |
| 可测试性 | 一般 | 优秀 |

---

## 迁移步骤

### 第1步: 依赖注入更新

#### 旧版代码
```java
@Autowired
SocketService socketService;
```

#### 新版代码
```java
@Autowired
SocketServiceRefactored socketService;
```

### 第2步: 方法调用保持不变

好消息！新版本保持了与旧版本相同的公共API，大部分方法调用无需修改：

```java
// 特征提取 - 无需修改
SocketFaceFeature feature = socketService.faceExtractFeature(images);

// 人脸比对 - 无需修改
SocketRecogResult result = socketService.faceCompareFeatToFeat(feat1, feat2);

// 质量检测 - 无需修改
SocketFaceDetectResult quality = socketService.faceQualityCheck(images, null);
```

### 第3步: 更新DTO类引用（如有嵌套类使用）

#### 旧版代码（如果使用了嵌套类）
```java
SocketResponse.FaceFeatureValue.Attachment attachment = ...;
SocketResponse.FaceFeatureValue.FeatureData featureData = ...;
SocketResponse.FaceDetectValue.FaceInfo faceInfo = ...;
```

#### 新版代码
```java
FaceFeatureAttachment attachment = ...;
FeatureData featureData = ...;
FaceInfo faceInfo = ...;
```

---

## 详细迁移示例

### 示例1: 人脸识别控制器

#### 旧版代码
```java
@RestController
@RequestMapping("/api/v1/faceRecog")
public class FaceRecogController {
    
    @Autowired
    SocketService socketService;
    
    @PostMapping("/extract")
    public ResponseEntity<?> extractFeature(@RequestParam String imageBase64) {
        try {
            JSONObject images = new JSONObject();
            images.put("0", imageBase64);
            
            SocketFaceFeature result = socketService.faceExtractFeature(images, true, false);
            
            if (result.getReturnId() != 0) {
                return ResponseEntity.badRequest().body(result.getReturnDesc());
            }
            
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body("算法服务异常");
        }
    }
}
```

#### 新版代码（推荐）
```java
@RestController
@RequestMapping("/api/v1/faceRecog")
public class FaceRecogController {
    
    @Autowired
    SocketServiceRefactored socketService;
    
    @PostMapping("/extract")
    public ResponseEntity<?> extractFeature(@RequestParam String imageBase64) {
        try {
            JSONObject images = new JSONObject();
            images.put("0", imageBase64);
            
            SocketFaceFeature result = socketService.faceExtractFeature(images, true, false);
            
            if (result.getReturnId() != 0) {
                return ResponseEntity.badRequest().body(result.getReturnDesc());
            }
            
            return ResponseEntity.ok(result);
            
        } catch (SocketConnectionException e) {
            log.error("Socket连接失败", e);
            return ResponseEntity.status(503).body("算法服务不可用");
        } catch (AlgoProcessException e) {
            log.error("算法处理失败: {}", e.getMessage());
            return ResponseEntity.status(500).body("算法处理失败");
        } catch (AlgoException e) {
            log.error("算法服务异常", e);
            return ResponseEntity.status(500).body("算法服务异常");
        }
    }
}
```

**主要改进**:
1. 依赖注入从 `SocketService` 改为 `SocketServiceRefactored`
2. 异常处理更精确，可以区分连接异常和处理异常
3. 返回更准确的HTTP状态码

### 示例2: 批量人脸比对

#### 旧版代码
```java
@Service
public class FaceCompareService {
    
    @Autowired
    private SocketService socketService;
    
    public List<CompareResult> batchCompare(String targetFeature, List<String> candidateFeatures) {
        List<CompareResult> results = new ArrayList<>();
        
        JSONObject targetFeat = new JSONObject();
        targetFeat.put("0", targetFeature);
        
        for (String candidate : candidateFeatures) {
            JSONObject candidateFeat = new JSONObject();
            candidateFeat.put("0", candidate);
            
            try {
                SocketRecogResult result = socketService.faceCompareFeatToFeat(targetFeat, candidateFeat);
                results.add(convertToCompareResult(result));
            } catch (RuntimeException e) {
                log.error("比对失败", e);
                // 继续下一个
            }
        }
        
        return results;
    }
}
```

#### 新版代码
```java
@Service
public class FaceCompareService {
    
    @Autowired
    private SocketServiceRefactored socketService;
    
    public List<CompareResult> batchCompare(String targetFeature, List<String> candidateFeatures) {
        List<CompareResult> results = new ArrayList<>();
        
        JSONObject targetFeat = new JSONObject();
        targetFeat.put("0", targetFeature);
        
        for (String candidate : candidateFeatures) {
            JSONObject candidateFeat = new JSONObject();
            candidateFeat.put("0", candidate);
            
            try {
                SocketRecogResult result = socketService.faceCompareFeatToFeat(targetFeat, candidateFeat);
                results.add(convertToCompareResult(result));
                
            } catch (SocketConnectionException e) {
                log.error("Socket连接失败，终止批量比对", e);
                break;  // 连接失败，不再继续
                
            } catch (AlgoProcessException e) {
                log.warn("单个比对失败: {}", e.getMessage());
                // 继续下一个
            }
        }
        
        return results;
    }
}
```

**主要改进**:
1. 精确的异常处理：连接异常时终止批处理，处理异常时继续
2. 更好的错误追踪和日志记录

---

## 新增功能使用

### 使用Builder Pattern构建请求（高级用法）

如果你需要添加自定义参数：

```java
import com.biometric.algo.builder.AlgoRequestBuilder;

@Service
public class CustomAlgoService {
    
    @Autowired
    private SocketClient socketClient;
    
    public String customAlgorithm(JSONObject images) {
        JSONObject params = AlgoRequestBuilder.newBuilder()
                .funId("Y99.99")  // 自定义功能ID
                .images(images)
                .imageNum(images.size())
                .algType(1)
                .version("CUSTOM_V1")
                .customParam("CUSTOM_KEY", "custom_value")  // 自定义参数
                .build();
        
        return socketClient.sendRequest(params);
    }
}
```

### 使用Strategy Pattern添加新比对策略

```java
import com.biometric.algo.strategy.FaceCompareStrategy;

public class CustomCompareStrategy extends FaceCompareStrategy {
    
    public CustomCompareStrategy(SocketClient socketClient) {
        super(socketClient);
    }
    
    @Override
    protected JSONObject buildParams(JSONObject data1, JSONObject data2, String version) {
        return AlgoRequestBuilder.newBuilder()
                .funId(getFunctionId())
                .algType(ALG_TYPE_FACE_VISIBLE)
                .version(version)
                .customParam("CUSTOM_COMPARE", true)
                .pFeature1(AlgoRequestBuilder.buildFeatureGroup(data1))
                .pFeature2(AlgoRequestBuilder.buildFeatureGroup(data2))
                .build();
    }
    
    @Override
    public String getFunctionId() {
        return "Y00.99";
    }
}
```

---

## 异常处理最佳实践

### 推荐的异常处理模式

```java
@Service
public class FaceRecognitionService {
    
    @Autowired
    private SocketServiceRefactored socketService;
    
    public ProcessResult processImage(String imageBase64) {
        try {
            JSONObject images = new JSONObject();
            images.put("0", imageBase64);
            
            // 1. 提取特征
            SocketFaceFeature feature = socketService.faceExtractFeature(images);
            
            // 2. 质量检测
            SocketFaceDetectResult quality = socketService.faceQualityCheck(images, null);
            
            // 3. 处理结果
            return ProcessResult.success(feature, quality);
            
        } catch (SocketConnectionException e) {
            log.error("连接算法引擎失败: {}", e.getMessage(), e);
            return ProcessResult.error("SERVICE_UNAVAILABLE", "算法服务暂时不可用");
            
        } catch (AlgoProcessException e) {
            log.error("算法处理失败 [code={}]: {}", e.getErrorCode(), e.getMessage());
            return ProcessResult.error("ALGO_ERROR", e.getMessage());
            
        } catch (AlgoException e) {
            log.error("算法服务异常: {}", e.getMessage(), e);
            return ProcessResult.error("UNKNOWN_ERROR", "处理失败");
        }
    }
}
```

---

## 配置更新

### application.yml 配置（无需修改）

```yaml
biometric:
  algo:
    socket:
      host: 192.168.10.250
      port: 9098
      timeout: 60000
      default-face-version: FACE310
      default-finger-version: FINGER30
      default-fingerprint-version: FINGERPRINT30
```

配置类 `AlgoSocketConfig` 保持不变，新旧版本共用。

---

## 单元测试

### 旧版测试
```java
@SpringBootTest
class SocketServiceTest {
    
    @Autowired
    private SocketService socketService;
    
    @Test
    void testFaceExtract() {
        JSONObject images = new JSONObject();
        images.put("0", testImageBase64);
        
        SocketFaceFeature result = socketService.faceExtractFeature(images);
        
        assertNotNull(result);
        assertEquals(0, result.getReturnId());
    }
}
```

### 新版测试（推荐）
```java
@SpringBootTest
class SocketServiceRefactoredTest {
    
    @Autowired
    private SocketServiceRefactored socketService;
    
    @Test
    void testFaceExtract() {
        JSONObject images = new JSONObject();
        images.put("0", testImageBase64);
        
        SocketFaceFeature result = socketService.faceExtractFeature(images);
        
        assertNotNull(result);
        assertEquals(0, result.getReturnId());
    }
    
    @Test
    void testSocketConnectionException() {
        // 模拟连接失败
        assertThrows(SocketConnectionException.class, () -> {
            // ... test code
        });
    }
}
```

---

## 性能影响

新版本的性能影响分析：

| 指标 | 影响 | 说明 |
|------|------|------|
| 响应时间 | 相同 | 核心网络通信逻辑未变 |
| 内存占用 | 略增 | Strategy对象缓存（可忽略） |
| CPU使用率 | 相同 | 算法逻辑未变 |
| 代码质量 | 显著提升 | 使用设计模式 |

**结论**: 新版本在保持相同性能的前提下，显著提升了代码质量。

---

## 常见问题

### Q1: 必须立即迁移吗？
**A**: 不必须。旧版 `SocketService` 继续可用，可以渐进式迁移。

### Q2: 新旧版本可以共存吗？
**A**: 可以。Spring容器会管理两个不同的Bean，互不影响。

### Q3: 如果遇到兼容性问题怎么办？
**A**: 
1. 检查DTO类的嵌套类引用是否需要更新
2. 查看异常处理是否需要更精确
3. 确认配置文件路径正确

### Q4: 如何回滚？
**A**: 只需将依赖注入改回 `SocketService` 即可，无需其他修改。

### Q5: 性能会受影响吗？
**A**: 不会。新版本主要是代码组织方式的改进，核心算法逻辑完全相同。

---

## 推荐迁移时间表

### 第1周: 评估和准备
- 阅读重构文档
- 评估现有代码使用情况
- 制定迁移计划

### 第2周: 开发环境测试
- 在开发环境更新依赖注入
- 运行单元测试
- 进行功能测试

### 第3周: 测试环境部署
- 部署到测试环境
- 进行集成测试
- 性能测试

### 第4周: 生产环境部署
- 灰度发布（部分服务器先切换）
- 监控日志和性能指标
- 全量切换

---

## 技术支持

如有问题，请查看：
1. [REFACTORING_GUIDE.md](REFACTORING_GUIDE.md) - 完整的设计模式说明
2. 源代码注释
3. 联系开发团队

---

## 总结

✅ **向后兼容**: 公共API保持不变  
✅ **渐进式迁移**: 新旧版本可共存  
✅ **低风险**: 可随时回滚  
✅ **高收益**: 代码质量显著提升  

建议尽早迁移到新版本，享受更好的代码质量和可维护性！

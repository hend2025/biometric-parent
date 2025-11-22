# Socket Service 重构指南

## 设计模式应用总结

本次重构采用了多种常用的Java设计模式来优化 `SocketService` 及相关实体类，提高代码的可维护性、可扩展性和可读性。

---

## 1. Builder Pattern (建造者模式)

### 类: `AlgoRequestBuilder`

**目的**: 优雅地构建复杂的算法请求参数

**优势**:
- 避免构造函数参数过多
- 代码更易读，参数设置更清晰
- 支持链式调用
- 参数可选，灵活性强

**示例**:
```java
// 重构前
JSONObject params = new JSONObject();
params.put("IMAGES", images);
params.put("NUM", images.size());
params.put("ALGTYPE", String.valueOf(ALG_TYPE_FACE_VISIBLE));
params.put("FUNID", "Y01.00");
params.put("ROTATE", rotate);
params.put("QUALITY", needQuality);
params.put("VERSION", config.getDefaultFaceVersion());

// 重构后
JSONObject params = AlgoRequestBuilder.newBuilder()
        .funId("Y01.00")
        .images(images)
        .imageNum(images.size())
        .algType(String.valueOf(ALG_TYPE_FACE_VISIBLE))
        .rotate(rotate)
        .quality(needQuality)
        .version(config.getDefaultFaceVersion())
        .build();
```

---

## 2. Strategy Pattern (策略模式)

### 类: `ComparisonStrategy` 接口及其实现类

**目的**: 将不同的比对算法封装成独立的策略

**优势**:
- 消除重复代码
- 易于添加新的比对策略
- 符合开闭原则（对扩展开放，对修改关闭）
- 便于单元测试

**实现**:
- `ComparisonStrategy` - 策略接口
- `FaceCompareStrategy` - 抽象基类（模板方法）
- `FeatureToFeatureStrategy` - 特征对特征
- `FeatureToImageStrategy` - 特征对图片
- `ImageToImageStrategy` - 图片对图片

**示例**:
```java
// 重构前: 三个方法有大量重复代码
public SocketRecogResult faceCompareFeatToFeat(...) {
    JSONObject params = new JSONObject();
    params.put("PFEATURE1", buildGroup(...));
    params.put("PFEATURE2", buildGroup(...));
    params.put("ALGTYPE", ALG_TYPE_FACE_VISIBLE);
    params.put("FUNID", "Y00.00");
    // ... 发送请求和解析响应
}

// 重构后: 使用策略模式
ComparisonStrategy strategy = comparisonStrategies.get("FEAT_TO_FEAT");
return strategy.compare(featureMap1, featureMap2, version);
```

---

## 3. Factory Pattern (工厂模式)

### 类: `ResponseFactory`

**目的**: 统一创建和解析响应对象

**优势**:
- 集中处理异常
- 统一响应验证逻辑
- 减少重复的JSON解析代码
- 便于统一错误处理

**示例**:
```java
// 重构前: 每个方法都有重复的解析逻辑
SocketFaceFeature result = new SocketFaceFeature();
try {
    String jsonResponse = sendRequest(params);
    result = JSON.parseObject(jsonResponse, SocketFaceFeature.class);
} catch (Exception e) {
    result.setReturnId(-1);
    result.setReturnDesc(e.getMessage());
    e.printStackTrace();
}

// 重构后
String jsonResponse = socketClient.sendRequest(params);
return ResponseFactory.parseFaceFeature(jsonResponse);
```

---

## 4. Template Method Pattern (模板方法模式)

### 类: `FaceCompareStrategy`

**目的**: 定义算法骨架，子类实现具体细节

**优势**:
- 复用公共代码
- 固定算法流程
- 子类只需关注差异部分

**实现**:
```java
public abstract class FaceCompareStrategy implements ComparisonStrategy {
    
    @Override
    public SocketRecogResult compare(JSONObject data1, JSONObject data2, String version) {
        JSONObject params = buildParams(data1, data2, version);  // 模板方法
        String jsonResponse = socketClient.sendRequest(params);
        return JSON.parseObject(jsonResponse, SocketRecogResult.class);
    }
    
    // 子类实现具体的参数构建逻辑
    protected abstract JSONObject buildParams(JSONObject data1, JSONObject data2, String version);
}
```

---

## 5. Single Responsibility Principle (单一职责原则)

### 重构的DTO类

**目的**: 每个类只负责一个功能

**重构内容**:
- 将 `SocketResponse` 中的嵌套类拆分为独立的类
- `FaceFeatureAttachment` - 特征附件信息
- `FeatureTypeInfo` - 特征类型信息
- `FeatureData` - 特征数据
- `FaceFeatureValue` - 人脸特征值
- `RecogValue` - 识别结果值
- `FaceInfo` - 人脸信息
- `FaceDetectValue` - 人脸检测值

**优势**:
- 更清晰的类结构
- 便于维护和测试
- 减少类之间的耦合
- 提高代码可读性

---

## 6. Improved Resource Management (改进的资源管理)

### 类: `SocketClient`

**目的**: 使用 try-with-resources 自动管理资源

**优势**:
- 自动关闭资源，避免资源泄漏
- 代码更简洁
- 异常处理更安全
- 符合Java最佳实践

**示例**:
```java
// 重构前: 手动管理资源
Socket socket = null;
PrintWriter out = null;
BufferedReader in = null;
try {
    socket = new Socket();
    // ... 使用资源
} finally {
    try {
        if (out != null) out.close();
        if (in != null) in.close();
        if (socket != null) socket.close();
    } catch (IOException ex) {
        log.warn("Error closing socket resources", ex);
    }
}

// 重构后: try-with-resources
try (Socket socket = createSocket();
     PrintWriter out = new PrintWriter(...);
     BufferedReader in = new BufferedReader(...)) {
    // ... 使用资源
    // 资源会自动关闭
}
```

---

## 7. Custom Exception Hierarchy (自定义异常体系)

### 类: `AlgoException`, `SocketConnectionException`, `AlgoProcessException`

**目的**: 提供更精确的异常信息

**优势**:
- 区分不同类型的错误
- 便于异常处理和调试
- 提供错误码支持
- 更好的错误追踪

**层次结构**:
```
AlgoException (基础异常)
├── SocketConnectionException (连接异常)
└── AlgoProcessException (处理异常)
```

**示例**:
```java
// 重构前
throw new RuntimeException("Failed to communicate with biometric algo engine", e);

// 重构后
throw new SocketConnectionException("Failed to communicate with biometric algo engine", e);
```

---

## 8. Singleton Pattern (单例模式)

### 使用: Spring的 `@Service` 和 `@Component` 注解

**目的**: 确保只有一个实例，节省资源

**实现**:
- `SocketServiceRefactored` 使用 `@Service`
- `SocketClient` 使用 `@Component`
- Spring容器管理生命周期

---

## 代码结构对比

### 重构前
```
service/
└── SocketService.java (362行，包含所有逻辑)

dto/
├── SocketResponse.java (147行，包含多个嵌套类)
└── 其他简单DTO类
```

### 重构后
```
service/
├── SocketService.java (原始版本，保留向后兼容)
└── SocketServiceRefactored.java (新版本，使用设计模式)

builder/
└── AlgoRequestBuilder.java (请求构建器)

strategy/
├── ComparisonStrategy.java (策略接口)
├── FaceCompareStrategy.java (抽象策略)
├── FeatureToFeatureStrategy.java
├── FeatureToImageStrategy.java
└── ImageToImageStrategy.java

factory/
└── ResponseFactory.java (响应工厂)

socket/
└── SocketClient.java (Socket通信客户端)

exception/
├── AlgoException.java (基础异常)
├── SocketConnectionException.java
└── AlgoProcessException.java

dto/
├── SocketResponse.java (简化版)
├── FaceFeatureAttachment.java
├── FeatureTypeInfo.java
├── FeatureData.java
├── FaceFeatureValue.java
├── RecogValue.java
├── FaceInfo.java
├── FaceDetectValue.java
└── deserializer/
    └── StringToObjectDeserializer.java
```

---

## 使用建议

### 迁移方案

1. **渐进式迁移**: 新代码使用 `SocketServiceRefactored`，旧代码继续使用 `SocketService`
2. **逐步替换**: 测试通过后，逐步将业务代码切换到新版本
3. **并行运行**: 两个版本可以共存，确保平滑过渡

### 使用示例

```java
@Service
public class FaceRecognitionService {
    
    @Autowired
    private SocketServiceRefactored socketService;
    
    public void processImage(String imageBase64) {
        JSONObject images = new JSONObject();
        images.put("0", imageBase64);
        
        // 提取特征
        SocketFaceFeature feature = socketService.faceExtractFeature(images);
        
        // 质量检测
        SocketFaceDetectResult quality = socketService.faceQualityCheck(images, null);
        
        // 特征比对
        SocketRecogResult result = socketService.faceCompareFeatToFeat(feature1, feature2);
    }
}
```

---

## 性能考虑

1. **Strategy缓存**: 策略对象在初始化时创建，避免重复创建
2. **响应解析**: Factory统一处理，减少重复代码
3. **资源管理**: try-with-resources确保资源及时释放
4. **异常处理**: 统一的异常体系，减少不必要的try-catch

---

## 扩展性

### 添加新的比对策略
```java
public class NewComparisonStrategy extends FaceCompareStrategy {
    @Override
    protected JSONObject buildParams(JSONObject data1, JSONObject data2, String version) {
        // 实现新的参数构建逻辑
    }
    
    @Override
    public String getFunctionId() {
        return "Y00.XX";
    }
}

// 在SocketServiceRefactored中注册
strategies.put("NEW_TYPE", new NewComparisonStrategy(socketClient));
```

### 添加新的算法功能
```java
public String newAlgorithmFunction(JSONObject params) {
    JSONObject requestParams = AlgoRequestBuilder.newBuilder()
            .funId("Y05.00")
            .customParam("CUSTOM_PARAM", "value")
            .build();
    
    return socketClient.sendRequest(requestParams);
}
```

---

## 总结

本次重构通过应用多种设计模式，显著提升了代码质量：

✅ **可维护性**: 代码结构清晰，职责分明  
✅ **可扩展性**: 易于添加新功能  
✅ **可测试性**: 各模块独立，便于单元测试  
✅ **健壮性**: 完善的异常处理和资源管理  
✅ **可读性**: 使用Builder模式提升代码可读性  

重构后的代码遵循SOLID原则，是企业级Java应用的最佳实践。

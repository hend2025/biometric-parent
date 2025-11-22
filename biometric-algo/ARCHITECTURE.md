# 架构设计文档

## 系统架构对比

### 重构前架构

```
┌─────────────────────────────────────────┐
│         FaceRecogController             │
│   (业务控制器 - 依赖SocketService)        │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│          SocketService                  │
│  ┌─────────────────────────────────┐   │
│  │ • 362行单体类                    │   │
│  │ • 所有功能混在一起                 │   │
│  │ • 大量重复代码                    │   │
│  │ • 手动资源管理                    │   │
│  │ • 通用异常处理                    │   │
│  │ • buildGroup()                  │   │
│  │ • sendRequest()                 │   │
│  │ • 20+个业务方法                   │   │
│  └─────────────────────────────────┘   │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│        AlgoSocketConfig                 │
│      (配置类 - 连接参数)                  │
└─────────────────────────────────────────┘
```

### 重构后架构（分层设计）

```
┌──────────────────────────────────────────────────────────┐
│                   Presentation Layer                     │
│            (表现层 - Controller/Service)                  │
│                                                          │
│   FaceRecogController, FaceRecognitionService, etc.     │
└───────────────────────┬──────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────┐
│                    Service Layer                         │
│                 (服务层 - 业务逻辑)                        │
│                                                          │
│              SocketServiceRefactored                     │
│   ┌────────────────────────────────────────────┐        │
│   │  • 人脸比对 (使用Strategy)                  │        │
│   │  • 特征提取 (使用Builder + Factory)         │        │
│   │  • 人脸处理 (使用Builder + Factory)         │        │
│   │  • 质量检测 (使用Builder + Factory)         │        │
│   └────────────────────────────────────────────┘        │
└──────┬────────────┬────────────┬──────────────┬──────────┘
       │            │            │              │
       ▼            ▼            ▼              ▼
┌─────────┐  ┌──────────┐  ┌─────────┐  ┌─────────────┐
│Strategy │  │ Builder  │  │ Factory │  │   Socket    │
│ Pattern │  │ Pattern  │  │ Pattern │  │   Client    │
└─────────┘  └──────────┘  └─────────┘  └─────────────┘
       │            │            │              │
       │            │            │              ▼
       │            │            │      ┌─────────────────┐
       │            │            │      │   Try-with-     │
       │            │            │      │   Resources     │
       │            │            │      └─────────────────┘
       │            │            │
       ▼            ▼            ▼
┌──────────────────────────────────────────────────────────┐
│                    Data Layer                            │
│                 (数据层 - DTO/Entity)                      │
│                                                          │
│  SocketResponse, FaceFeatureValue, RecogValue, etc.     │
└──────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│              Exception Hierarchy                         │
│               (异常体系 - 错误处理)                        │
│                                                          │
│  AlgoException → SocketConnectionException               │
│               → AlgoProcessException                     │
└──────────────────────────────────────────────────────────┘
```

---

## 设计模式关系图

```
                    ┌─────────────────────┐
                    │  SocketService      │
                    │  Refactored         │
                    └──────────┬──────────┘
                               │
            ┌──────────────────┼──────────────────┐
            │                  │                  │
            ▼                  ▼                  ▼
    ┌───────────────┐  ┌──────────────┐  ┌──────────────┐
    │  Comparison   │  │   Request    │  │   Response   │
    │  Strategies   │  │   Builder    │  │   Factory    │
    │  (Strategy)   │  │  (Builder)   │  │  (Factory)   │
    └───────┬───────┘  └──────┬───────┘  └──────┬───────┘
            │                  │                  │
    ┌───────┴────────┐        │                  │
    │                │         │                  │
    ▼                ▼         ▼                  ▼
┌────────┐  ┌────────────┐  ┌──────────┐  ┌──────────┐
│ Feat2  │  │ Feat2Img  │  │  Algo    │  │  Parse   │
│  Feat  │  │  Strategy │  │ Request  │  │ Response │
└────────┘  └───────────┘  │  Params  │  │  Object  │
┌────────┐                 └──────────┘  └──────────┘
│ Img2   │
│  Img   │
└────────┘
            │
            ▼
    ┌──────────────┐
    │ Socket       │
    │ Client       │
    │ (Resource    │
    │  Management) │
    └──────┬───────┘
           │
           ▼
    ┌──────────────┐
    │ Exception    │
    │ Hierarchy    │
    └──────────────┘
```

---

## 类图 - Strategy Pattern

```
┌─────────────────────────────────┐
│   <<interface>>                 │
│   ComparisonStrategy            │
├─────────────────────────────────┤
│ + compare(data1, data2,        │
│          version): Result       │
│ + getFunctionId(): String       │
└────────────┬────────────────────┘
             │
             │ implements
             ▼
┌─────────────────────────────────┐
│   <<abstract>>                  │
│   FaceCompareStrategy           │
├─────────────────────────────────┤
│ # socketClient: SocketClient    │
├─────────────────────────────────┤
│ + compare(...): Result          │
│ # buildParams(...): JSONObject  │◄─────┐
└─────────────────────────────────┘      │
             ▲                            │ extends
             │                            │
     ┌───────┼───────┬────────────────────┘
     │       │       │
     │       │       │
┌────┴────┐ │ ┌─────┴────────┐  ┌────────────────┐
│Feature  │ │ │FeatureToImage│  │  ImageToImage  │
│   To    │ │ │   Strategy   │  │    Strategy    │
│Feature  │ │ └──────────────┘  └────────────────┘
│Strategy │ │
└─────────┘ │
            │
            └─ (更多策略可扩展)
```

---

## 类图 - Builder Pattern

```
┌─────────────────────────────────────┐
│      AlgoRequestBuilder             │
├─────────────────────────────────────┤
│ - params: JSONObject                │
├─────────────────────────────────────┤
│ + newBuilder(): Builder             │ ◄─── Static Factory
│ + funId(String): Builder            │ ◄─┐
│ + algType(int): Builder             │   │
│ + version(String): Builder          │   │ 链式调用
│ + images(JSONObject): Builder       │   │
│ + imageNum(int): Builder            │   │
│ + rotate(boolean): Builder          │   │
│ + quality(boolean): Builder         │ ◄─┘
│ + ... (more setters)                │
│ + build(): JSONObject               │ ◄─── Build
└─────────────────────────────────────┘

使用示例:
┌────────────────────────────────────────────┐
│ AlgoRequestBuilder.newBuilder()            │
│     .funId("Y01.00")                       │
│     .images(images)                        │
│     .imageNum(images.size())               │
│     .algType("1")                          │
│     .rotate(true)                          │
│     .quality(false)                        │
│     .version("FACE310")                    │
│     .build()                               │
└────────────────────────────────────────────┘
```

---

## 类图 - Factory Pattern

```
┌─────────────────────────────────────────┐
│       ResponseFactory                   │
│         (Static Factory)                │
├─────────────────────────────────────────┤
│ + parseRecogResult(json): RecogResult   │
│ + parseFaceFeature(json): FaceFeature   │
│ + parseFaceDetect(json): FaceDetect     │
│ - validateResponse(response): void      │
└────────────┬────────────────────────────┘
             │
             │ creates
             ▼
┌─────────────────────────────────────────┐
│        Response Objects                 │
├─────────────────────────────────────────┤
│ • SocketRecogResult                     │
│ • SocketFaceFeature                     │
│ • SocketFaceDetectResult                │
└─────────────────────────────────────────┘
```

---

## 类图 - Exception Hierarchy

```
┌─────────────────────────────────────┐
│     RuntimeException                │
│     (Java标准异常)                   │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│        AlgoException                │
│       (算法基础异常)                  │
├─────────────────────────────────────┤
│ - errorCode: int                    │
├─────────────────────────────────────┤
│ + AlgoException(message)            │
│ + AlgoException(code, message)      │
│ + getErrorCode(): int               │
└──────────┬─────────────┬────────────┘
           │             │
    ┌──────▼──────┐ ┌───▼──────────────┐
    │   Socket    │ │    Algo          │
    │ Connection  │ │   Process        │
    │  Exception  │ │  Exception       │
    │(连接异常)    │ │ (处理异常)        │
    └─────────────┘ └──────────────────┘

使用场景:
• SocketConnectionException → 网络连接失败
• AlgoProcessException → 算法处理失败 (带错误码)
```

---

## 序列图 - 人脸特征提取流程

```
Controller   Service   Builder   SocketClient   Factory   AlgoEngine
    │           │         │            │            │          │
    │─────────►│         │            │            │          │
    │ extract  │         │            │            │          │
    │          │         │            │            │          │
    │          │────────►│            │            │          │
    │          │ build   │            │            │          │
    │          │ request │            │            │          │
    │          │         │            │            │          │
    │          │◄────────│            │            │          │
    │          │ params  │            │            │          │
    │          │         │            │            │          │
    │          │─────────────────────►│            │          │
    │          │      sendRequest     │            │          │
    │          │                      │            │          │
    │          │                      │─────────────────────►│
    │          │                      │    Socket Request    │
    │          │                      │                      │
    │          │                      │◄─────────────────────│
    │          │                      │    JSON Response     │
    │          │                      │                      │
    │          │─────────────────────────────────►│          │
    │          │            parseFeature           │          │
    │          │                                   │          │
    │          │◄──────────────────────────────────│          │
    │          │          FeatureResult            │          │
    │          │                                   │          │
    │◄─────────│                                   │          │
    │  result  │                                   │          │
```

---

## 序列图 - 人脸比对流程 (Strategy Pattern)

```
Controller   Service   Strategy Map   Strategy   SocketClient   AlgoEngine
    │           │            │            │            │             │
    │──────────►│            │            │            │             │
    │ compare   │            │            │            │             │
    │           │            │            │            │             │
    │           │───────────►│            │            │             │
    │           │ get("FEAT_ │            │            │             │
    │           │   TO_FEAT") │           │            │             │
    │           │            │            │            │             │
    │           │◄───────────│            │            │             │
    │           │  strategy  │            │            │             │
    │           │            │            │            │             │
    │           │────────────────────────►│            │             │
    │           │      compare(...)       │            │             │
    │           │                         │            │             │
    │           │                         │───────────►│             │
    │           │                         │ buildParams│             │
    │           │                         │            │             │
    │           │                         │───────────────────────►│
    │           │                         │     sendRequest        │
    │           │                         │                        │
    │           │                         │◄──────────────────────│
    │           │                         │     JSON Response     │
    │           │                         │                        │
    │           │◄────────────────────────│                        │
    │           │       RecogResult       │                        │
    │           │                         │                        │
    │◄──────────│                         │                        │
    │  result   │                         │                        │
```

---

## 数据流图

```
┌─────────────┐
│  Client     │
│  Request    │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────┐
│  SocketServiceRefactored        │
│  (Facade/Coordinator)           │
└──────┬──────────────────────────┘
       │
       ├─────────────┐
       │             │
       ▼             ▼
┌──────────┐   ┌──────────────┐
│ Strategy │   │   Builder    │
│  Select  │   │   Create     │
│          │   │   Request    │
└────┬─────┘   └──────┬───────┘
     │                │
     └────────┬───────┘
              │
              ▼
        ┌───────────┐
        │  Socket   │
        │  Client   │
        └─────┬─────┘
              │
              ▼
        ┌───────────┐
        │  Network  │
        │   (TCP)   │
        └─────┬─────┘
              │
              ▼
        ┌───────────┐
        │   Algo    │
        │  Engine   │
        └─────┬─────┘
              │
              ▼
        ┌───────────┐
        │ Response  │
        │   JSON    │
        └─────┬─────┘
              │
              ▼
        ┌───────────┐
        │  Factory  │
        │  Parse    │
        └─────┬─────┘
              │
              ▼
        ┌───────────┐
        │   DTO     │
        │  Object   │
        └─────┬─────┘
              │
              ▼
        ┌───────────┐
        │  Return   │
        │to Client  │
        └───────────┘
```

---

## 模块依赖关系

```
┌────────────────────────────────────────────────┐
│            Application Layer                   │
│   (Controllers, Application Services)          │
└─────────────────┬──────────────────────────────┘
                  │ depends on
                  ▼
┌────────────────────────────────────────────────┐
│            Service Layer                       │
│      SocketServiceRefactored                   │
└──┬────────┬────────┬────────┬──────────────────┘
   │        │        │        │
   │ uses   │ uses   │ uses   │ uses
   ▼        ▼        ▼        ▼
┌────────┐┌────────┐┌────────┐┌────────────┐
│Strategy││Builder ││Factory ││   Socket   │
│Pattern ││Pattern ││Pattern ││   Client   │
└────────┘└────────┘└────────┘└─────┬──────┘
                                     │ uses
                                     ▼
                              ┌─────────────┐
                              │   Config    │
                              └─────────────┘

┌────────────────────────────────────────────────┐
│             Data Layer                         │
│        DTO Classes & Entities                  │
└────────────────────────────────────────────────┘

┌────────────────────────────────────────────────┐
│          Infrastructure Layer                  │
│       Exception Hierarchy, Utils               │
└────────────────────────────────────────────────┘
```

---

## 扩展点示意图

```
新增比对策略扩展点:
┌─────────────────────────────────────┐
│   ComparisonStrategy Interface      │
└───────────────┬─────────────────────┘
                │
        ┌───────┴────────┬─────────────┐
        │                │             │
    Existing         Existing       ★ New
    Strategy         Strategy      Strategy
        │                │             │
        └────────────────┼─────────────┘
                         │
                         ▼
                  Strategy Pool
                  (Easy to extend)


新增算法功能扩展点:
┌─────────────────────────────────────┐
│      SocketServiceRefactored        │
│                                     │
│  + existingFeature1()               │
│  + existingFeature2()               │
│  + ★ newAlgorithmFeature()         │ ← 扩展点
│      └─ uses Builder                │
│      └─ uses Factory                │
│      └─ uses SocketClient           │
└─────────────────────────────────────┘


新增DTO扩展点:
┌─────────────────────────────────────┐
│     ResponseFactory                 │
│                                     │
│  + parseRecogResult()               │
│  + parseFaceFeature()               │
│  + ★ parseNewResponseType()        │ ← 扩展点
└─────────────────────────────────────┘
                │
                ▼
        ┌─────────────┐
        │ ★ NewDTO    │
        └─────────────┘
```

---

## 性能优化点

```
1. Strategy缓存
   ┌──────────────────────┐
   │  Strategy Map        │
   │  (Initialize Once)   │
   └──────────────────────┘
         │
         ▼
   ┌──────────────────────┐
   │  Reuse Instances     │
   │  (No Recreation)     │
   └──────────────────────┘


2. 连接管理
   ┌──────────────────────┐
   │  Socket Connection   │
   │  (On-demand)         │
   └──────────────────────┘
         │
         ▼
   ┌──────────────────────┐
   │  Try-with-resources  │
   │  (Auto-close)        │
   └──────────────────────┘


3. 异常处理
   ┌──────────────────────┐
   │  Early Validation    │
   └──────────────────────┘
         │
         ▼
   ┌──────────────────────┐
   │  Fail Fast           │
   │  (Reduce Overhead)   │
   └──────────────────────┘
```

---

## 总结

重构后的架构具有以下特点:

✅ **清晰的分层结构** - Service/Strategy/Factory/DTO分离  
✅ **松耦合设计** - 通过接口和抽象类解耦  
✅ **高内聚模块** - 每个类职责单一明确  
✅ **易于扩展** - 多个扩展点可平滑添加新功能  
✅ **便于测试** - 各层可独立进行单元测试  
✅ **资源安全** - 自动资源管理，防止泄漏  
✅ **精确异常** - 分层异常体系，准确定位问题  

这是一个遵循SOLID原则和企业级最佳实践的优秀架构设计。

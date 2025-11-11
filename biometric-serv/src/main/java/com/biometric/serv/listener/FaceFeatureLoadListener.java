//package com.biometric.serv.listener;
//
//import com.biometric.serv.service.FaceFeatureLoadService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.context.event.ApplicationReadyEvent;
//import org.springframework.context.ApplicationListener;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Component;
//
///**
// * 人脸特征加载监听器
// * 在应用启动完成后自动加载人脸特征数据到 Hazelcast
// */
//@Slf4j
//@Component
//public class FaceFeatureLoadListener implements ApplicationListener<ApplicationReadyEvent> {
//
//    @Autowired
//    private FaceFeatureLoadService faceFeatureLoadService;
//
//    @Value("${biometric.face.autoload:true}")
//    private boolean autoLoad;
//
//    @Value("${biometric.face.load.delay:5000}")
//    private long loadDelay;
//
//    @Override
//    @Async
//    public void onApplicationEvent(ApplicationReadyEvent event) {
//        if (!autoLoad) {
//            log.info("人脸特征自动加载已禁用");
//            return;
//        }
//
//        log.info("应用启动完成，准备加载人脸特征数据到 Hazelcast");
//
//        // 延迟执行，确保算法服务已准备好
//        try {
//            Thread.sleep(loadDelay);
//        } catch (InterruptedException e) {
//            log.error("延迟加载等待被中断", e);
//            Thread.currentThread().interrupt();
//        }
//
//        // 执行加载
//        try {
//            long totalCount = faceFeatureLoadService.getTotalFaceFeatureCount();
//            log.info("数据库中共有 {} 条有效人脸特征数据", totalCount);
//
//            if (totalCount > 0) {
//                faceFeatureLoadService.loadFaceFeaturesToHazelcast();
//            } else {
//                log.warn("数据库中没有有效的人脸特征数据需要加载");
//            }
//        } catch (Exception e) {
//            log.error("人脸特征数据加载失败", e);
//        }
//    }
//}
//

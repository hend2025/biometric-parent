package com.biometric.serv.listener;

import com.biometric.serv.service.FaceFeatureLoadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private FaceFeatureLoadService faceFeatureLoadService;

    @Value("${biometric.face.autoload:false}")
    private boolean autoload;

    @Override
    @Async
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!autoload) {
            log.info("自动加载人脸特征数据功能已禁用，如需加载请调用手动加载接口");
            return;
        }

        log.info("========== 应用启动完成，准备加载人脸特征数据 ==========");

        try {
            faceFeatureLoadService.loadFaceFeaturesToHazelcast();
        } catch (Exception e) {
            log.error("自动加载人脸特征数据失败", e);
        }
    }

}



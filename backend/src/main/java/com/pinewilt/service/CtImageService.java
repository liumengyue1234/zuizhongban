package com.pinewilt.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pinewilt.entity.CtImage;
import com.pinewilt.util.PythonServiceUtil.DetectionResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * CT影像Service接口
 */
public interface CtImageService extends IService<CtImage> {

    /**
     * 上传CT影像
     */
    CtImage uploadImage(MultipartFile file, Long userId);

    /**
     * 提交检测任务
     */
    DetectionResult detect(Long imageId, String modelName, String denoiseMethod);

    /**
     * 获取影像URL
     */
    String getImageUrl(Long imageId);
}

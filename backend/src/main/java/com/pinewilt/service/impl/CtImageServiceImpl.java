package com.pinewilt.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pinewilt.entity.CtImage;
import com.pinewilt.entity.DetectionRecord;
import com.pinewilt.mapper.CtImageMapper;
import com.pinewilt.mapper.DetectionRecordMapper;
import com.pinewilt.service.CtImageService;
import com.pinewilt.util.PythonServiceUtil.DetectionResult;
import com.pinewilt.util.PythonServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * CT影像Service实现类
 */
@Slf4j
@Service
public class CtImageServiceImpl extends ServiceImpl<CtImageMapper, CtImage> implements CtImageService {

    @Value("${file.upload-path}")
    private String uploadPath;

    @Value("${file.image-path}")
    private String imagePath;

    @Autowired
    private DetectionRecordMapper detectionRecordMapper;

    @Autowired
    private PythonServiceUtil pythonServiceUtil;

    @Override
    public CtImage uploadImage(MultipartFile file, Long userId) {
        // 确保目录存在
        File dir = new File(imagePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = UUID.randomUUID().toString() + extension;
        String filePath = imagePath + newFilename;

        // 保存文件
        try {
            file.transferTo(new File(filePath));
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new RuntimeException("文件保存失败");
        }

        // 保存数据库记录
        CtImage ctImage = new CtImage();
        ctImage.setImageName(originalFilename);
        ctImage.setFilePath(filePath);
        ctImage.setFileSize(formatFileSize(file.getSize()));
        ctImage.setFormat(extension.substring(1));
        ctImage.setStatus("pending");
        ctImage.setUserId(userId);
        ctImage.setDenoiseMethod("none");
        save(ctImage);

        return ctImage;
    }

    @Override
    public DetectionResult detect(Long imageId, String modelName, String denoiseMethod) {
        CtImage ctImage = getById(imageId);
        if (ctImage == null) {
            throw new RuntimeException("影像不存在");
        }

        // 更新状态为检测中
        ctImage.setStatus("processing");
        ctImage.setDenoiseMethod(denoiseMethod);
        updateById(ctImage);

        try {
            // 调用Python深度学习服务
            DetectionResult result = pythonServiceUtil.detect(
                ctImage.getFilePath(), modelName, denoiseMethod
            );

            // 更新影像状态
            ctImage.setStatus("detected");
            updateById(ctImage);

            // 保存检测记录
            DetectionRecord record = new DetectionRecord();
            record.setImageId(imageId);
            record.setImageName(ctImage.getImageName());
            record.setModelName(modelName);
            record.setResult(result.isPositive() ? "positive" : "negative");
            record.setDiceScore(result.getDiceScore());
            record.setIouScore(result.getIouScore());
            record.setPrecision(result.getPrecision());
            record.setRecall(result.getRecall());
            record.setF1Score(result.getF1Score());
            record.setAccuracy(result.getAccuracy());
            record.setSegmentationPath(result.getSegmentationPath());
            record.setOriginalImagePath(ctImage.getFilePath());
            record.setInferenceTime(result.getInferenceTime());
            record.setUserId(ctImage.getUserId());
            record.setDenoiseMethod(denoiseMethod);
            detectionRecordMapper.insert(record);

            return result;
        } catch (Exception e) {
            ctImage.setStatus("pending");
            updateById(ctImage);
            log.error("检测失败", e);
            throw new RuntimeException("检测失败: " + e.getMessage());
        }
    }

    @Override
    public String getImageUrl(Long imageId) {
        CtImage ctImage = getById(imageId);
        if (ctImage == null) {
            return null;
        }
        return "/api/images/view/" + imageId;
    }

    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + "B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1fKB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1fMB", size / (1024.0 * 1024));
        } else {
            return String.format("%.1fGB", size / (1024.0 * 1024 * 1024));
        }
    }
}

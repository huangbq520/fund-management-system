package com.fund.controller;

import com.fund.service.OcrService;
import com.fund.vo.ApiResponse;
import com.fund.vo.OcrResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/ocr")
@CrossOrigin(origins = "*")
public class OcrController {

    private static final Logger logger = LoggerFactory.getLogger(OcrController.class);

    @Resource
    private OcrService ocrService;

    @PostMapping("/recognize")
    public ApiResponse<OcrResult> recognizeFundCode(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        logger.info("API: OCR识别基金代码, userId={}, fileName={}, size={}",
            userId, file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            return ApiResponse.error("上传文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ApiResponse.error("只支持图片文件");
        }

        if (file.getSize() > 4 * 1024 * 1024) {
            return ApiResponse.error("图片大小不能超过4MB");
        }

        try {
            OcrResult result = ocrService.processImage(file);
            if (result.getFundCodes() != null && !result.getFundCodes().isEmpty()) {
                return ApiResponse.success("识别成功", result);
            } else if (result.getFundNames() != null && !result.getFundNames().isEmpty()) {
                return ApiResponse.success("未识别到基金代码，已提取基金名称", result);
            } else {
                return ApiResponse.error("未能从图片中识别到基金信息");
            }
        } catch (Exception e) {
            logger.error("OCR识别失败: {}", e.getMessage(), e);
            return ApiResponse.error("识别失败: " + e.getMessage());
        }
    }
}

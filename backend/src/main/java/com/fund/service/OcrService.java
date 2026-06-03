package com.fund.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.fund.util.BaiduOcrUtil;
import com.fund.vo.OcrResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OcrService {

    private static final Logger logger = LoggerFactory.getLogger(OcrService.class);
    private static final Pattern FUND_CODE_PATTERN = Pattern.compile("\\b\\d{6}\\b");

    @Resource
    private BaiduOcrUtil baiduOcrUtil;

    public OcrResult processImage(MultipartFile file) throws IOException {
        OcrResult result = new OcrResult();

        byte[] imageBytes = file.getBytes();
        String responseJson = baiduOcrUtil.recognizeText(imageBytes);

        if (responseJson == null) {
            throw new RuntimeException("百度 OCR 识别失败，请稍后重试");
        }

        JSONObject json = JSONObject.parseObject(responseJson);

        // 检查是否有错误
        if (json.containsKey("error_code")) {
            String errorMsg = json.getString("error_msg");
            logger.error("百度 OCR 返回错误: code={}, msg={}", json.get("error_code"), errorMsg);
            throw new RuntimeException("OCR 识别失败: " + errorMsg);
        }

        JSONArray wordsResult = json.getJSONArray("words_result");
        if (wordsResult == null || wordsResult.isEmpty()) {
            return result;
        }

        // 提取所有识别文字
        List<String> allWords = new ArrayList<>();
        for (int i = 0; i < wordsResult.size(); i++) {
            JSONObject item = wordsResult.getJSONObject(i);
            String word = item.getString("words");
            if (word != null) {
                allWords.add(word.trim());
            }
        }

        // 拼接原始文本
        result.setRawText(String.join(" ", allWords));

        // 优先提取6位基金代码
        Set<String> codeSet = new LinkedHashSet<>();
        for (String word : allWords) {
            Matcher matcher = FUND_CODE_PATTERN.matcher(word);
            while (matcher.find()) {
                codeSet.add(matcher.group());
            }
        }

        if (!codeSet.isEmpty()) {
            result.setFundCodes(new ArrayList<>(codeSet));
            return result;
        }

        // 未识别到基金代码，回退提取基金名称
        List<String> nameList = new ArrayList<>();
        for (String word : allWords) {
            if (word.length() >= 3 && word.length() <= 40 && containsChinese(word)) {
                nameList.add(word);
            }
        }
        result.setFundNames(nameList);

        return result;
    }

    private boolean containsChinese(String str) {
        for (char c : str.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }
}

package edu.ustb.eldercarebackend.controller.common;

import com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 阿里云百炼语音合成（TTS）测试接口
 * 用于测试文字转语音功能
 */
@RestController
@RequestMapping("/api/tts")
@CrossOrigin(origins = "*")
public class TtsController {

    private static final String MODEL = "qwen3-tts-flash";
    
    // 音频URL缓存（audioId -> ossUrl）
    private static final ConcurrentHashMap<String, String> audioCache = new ConcurrentHashMap<>();
    
    // 本地音频缓存目录
    private static final String AUDIO_CACHE_DIR = "tts_audio_cache";
    
    // 静态初始化块：创建缓存目录
    static {
        try {
            Path cachePath = Paths.get(AUDIO_CACHE_DIR);
            if (!Files.exists(cachePath)) {
                Files.createDirectories(cachePath);
                System.out.println("✅ TTS音频缓存目录已创建: " + cachePath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("❌ 创建TTS音频缓存目录失败: " + e.getMessage());
        }
    }

    /**
     * 语音合成接口
     * @param request 包含文本内容、音色、语言类型的请求
     * @return 包含音频URL的响应
     */
    @PostMapping("/synthesize")
    public Map<String, Object> synthesizeSpeech(@RequestBody TtsRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("🎵 [TTS] 收到合成请求");
            System.out.println("📝 [TTS] 文本长度: " + (request.getText() != null ? request.getText().length() : 0));
            System.out.println("🎤 [TTS] 音色: " + request.getVoice());
            System.out.println("🌍 [TTS] 语言: " + request.getLanguageType());
            
            // 验证输入
            if (request.getText() == null || request.getText().trim().isEmpty()) {
                System.err.println("❌ [TTS] 文本内容为空");
                response.put("success", false);
                response.put("message", "文本内容不能为空");
                return response;
            }

            System.out.println("🚀 [TTS] 调用阿里云API开始合成...");
            
            // 创建语音合成对象
            MultiModalConversation conv = new MultiModalConversation();
            
            // 构建请求参数
            MultiModalConversationParam param = MultiModalConversationParam.builder()
                    .model(MODEL)
                    .text(request.getText())
                    .voice(getVoiceEnum(request.getVoice()))
                    .languageType(request.getLanguageType())
                    .build();

            // 调用阿里云API
            MultiModalConversationResult result = conv.call(param);
            System.out.println("✅ [TTS] 阿里云API调用成功");
            
            // 获取阿里云OSS的原始URL
            String ossUrl = result.getOutput().getAudio().getUrl();
            System.out.println("🌐 [TTS] OSS URL: " + ossUrl);
            
            // 生成唯一的音频ID
            String audioId = String.valueOf(System.currentTimeMillis()) + "_" + Math.abs(request.getText().hashCode());
            System.out.println("🔑 [TTS] 生成音频ID: " + audioId);
            
            // 缓存OSS URL
            audioCache.put(audioId, ossUrl);
            System.out.println("💾 [TTS] OSS URL已缓存");
            
            // 返回代理URL（通过后端转发，避免跨域问题）
            String proxyUrl = "/api/tts/play/" + audioId;
            System.out.println("🔗 [TTS] 代理URL: " + proxyUrl);

            response.put("success", true);
            response.put("audioUrl", proxyUrl);  // 返回代理URL
            response.put("ossUrl", ossUrl);      // 同时返回原始OSS URL（调试用）
            response.put("message", "语音合成成功");
            
            System.out.println("✅ [TTS] 合成响应已准备完成");
            
        } catch (NoApiKeyException e) {
            System.err.println("❌ API Key 未配置或配置错误");
            response.put("success", false);
            response.put("message", "API Key 未配置，请检查环境变量 DASHSCOPE_API_KEY");
            
        } catch (ApiException e) {
            System.err.println("❌ API 调用失败：" + e.getMessage());
            response.put("success", false);
            response.put("message", "API调用失败：" + e.getMessage());
            
        } catch (UploadFileException e) {
            System.err.println("❌ 文件上传失败：" + e.getMessage());
            response.put("success", false);
            response.put("message", "文件上传失败：" + e.getMessage());
            
        } catch (Exception e) {
            System.err.println("❌ 未知错误：" + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "系统错误：" + e.getMessage());
        }
        
        return response;
    }

    /**
     * 音频代理转发接口（避免跨域和签名问题）
     * 支持本地文件缓存，提高播放速度
     * @param audioId 音频ID
     * @return 音频流
     */
    @GetMapping("/play/{audioId}")
    public ResponseEntity<byte[]> playAudio(@PathVariable String audioId) {
        try {
            System.out.println("📡 [TTS] 收到播放请求，audioId: " + audioId);
            
            // 1. 检查本地缓存文件是否存在
            File cachedFile = new File(AUDIO_CACHE_DIR, audioId + ".wav");
            byte[] audioData;
            
            if (cachedFile.exists()) {
                System.out.println("✅ [TTS] 从本地缓存读取音频: " + cachedFile.getAbsolutePath());
                // 从本地缓存读取
                try (FileInputStream fis = new FileInputStream(cachedFile)) {
                    audioData = fis.readAllBytes();
                }
                System.out.println("✅ [TTS] 本地缓存音频大小: " + audioData.length + " bytes");
                
            } else {
                System.out.println("⚠️ [TTS] 本地缓存不存在，从OSS下载");
                
                // 2. 从缓存获取OSS URL
                String ossUrl = audioCache.get(audioId);
                if (ossUrl == null) {
                    System.err.println("❌ [TTS] 音频ID不存在: " + audioId);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("音频不存在或已过期".getBytes());
                }
                
                System.out.println("🌐 [TTS] OSS URL: " + ossUrl);
                
                // 3. 从OSS下载音频
                HttpURLConnection connection = (HttpURLConnection) URI.create(ossUrl).toURL().openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);  // 增加超时时间到15秒
                connection.setReadTimeout(60000);     // 增加读取超时到60秒
                
                System.out.println("📥 [TTS] 开始从OSS下载音频...");
                int responseCode = connection.getResponseCode();
                System.out.println("📊 [TTS] OSS响应码: " + responseCode);
                
                if (responseCode != 200) {
                    System.err.println("❌ [TTS] OSS返回错误状态码: " + responseCode);
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                            .body("无法获取音频文件".getBytes());
                }
                
                // 4. 读取音频数据
                try (InputStream inputStream = connection.getInputStream()) {
                    audioData = inputStream.readAllBytes();
                }
                System.out.println("✅ [TTS] OSS音频下载完成，大小: " + audioData.length + " bytes");
                
                // 5. 保存到本地缓存
                try (FileOutputStream fos = new FileOutputStream(cachedFile)) {
                    fos.write(audioData);
                    System.out.println("💾 [TTS] 音频已缓存到本地: " + cachedFile.getAbsolutePath());
                } catch (IOException e) {
                    System.err.println("⚠️ [TTS] 保存本地缓存失败（不影响本次播放）: " + e.getMessage());
                }
            }
            
            // 6. 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/wav"));
            headers.setContentLength(audioData.length);
            headers.setCacheControl("public, max-age=3600");
            headers.set("Accept-Ranges", "bytes");
            
            System.out.println("✅ [TTS] 返回音频数据，大小: " + audioData.length + " bytes");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(audioData);
                    
        } catch (IOException e) {
            System.err.println("❌ [TTS] IO错误：" + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("下载音频失败: " + e.getMessage()).getBytes());
                    
        } catch (Exception e) {
            System.err.println("❌ [TTS] 未知错误：" + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("系统错误: " + e.getMessage()).getBytes());
        }
    }

    /**
     * 获取可用的音色列表
     * @return 音色列表
     */
    @GetMapping("/voices")
    public Map<String, Object> getVoices() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("voices", new String[]{
            "Cherry",    // 甜美女声（中英文）
            "Qwen",      // 温和男声（中英文）
            "Dylan",     // 成熟男声（英文）
            "Jada",      // 亲切女声（英文）
            "Sunny"      // 活力女声（英文）
        });
        return response;
    }

    /**
     * 将字符串音色转换为枚举
     */
    private AudioParameters.Voice getVoiceEnum(String voice) {
        if (voice == null || voice.trim().isEmpty()) {
            return AudioParameters.Voice.CHERRY;
        }
        
        try {
            return AudioParameters.Voice.valueOf(voice.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AudioParameters.Voice.CHERRY;
        }
    }

    /**
     * TTS 请求参数类
     */
    public static class TtsRequest {
        private String text;           // 要合成的文本
        private String voice;          // 音色（Cherry/Qwen/Dylan/Jada/Sunny）
        private String languageType;   // 语言类型（Chinese/English）

        // Constructors
        public TtsRequest() {
            this.voice = "Cherry";
            this.languageType = "Chinese";
        }

        // Getters and Setters
        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getVoice() {
            return voice;
        }

        public void setVoice(String voice) {
            this.voice = voice;
        }

        public String getLanguageType() {
            return languageType;
        }

        public void setLanguageType(String languageType) {
            this.languageType = languageType;
        }
    }
}

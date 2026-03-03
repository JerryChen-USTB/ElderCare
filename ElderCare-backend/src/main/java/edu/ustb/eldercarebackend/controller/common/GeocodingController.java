package edu.ustb.eldercarebackend.controller.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 高德地图逆地理编码接口
 * 将经纬度坐标转换为详细地址信息
 */
@RestController
@RequestMapping("/api/geocoding")
@CrossOrigin(origins = "*")
public class GeocodingController {

    // 高德地图 Web 服务 API Key（从配置文件读取）
    @Value("${amap.web-api-key}")
    private String amapWebKey;
    
    private static final String REGEO_URL = "https://restapi.amap.com/v3/geocode/regeo";

    /**
     * 逆地理编码：经纬度转地址
     * @param longitude 经度
     * @param latitude 纬度
     * @return 地址信息
     */
    @GetMapping("/regeo")
    public Map<String, Object> reverseGeocode(
            @RequestParam Double longitude,
            @RequestParam Double latitude,
            @RequestParam(required = false, defaultValue = "1000") Integer radius,
            @RequestParam(required = false, defaultValue = "base") String extensions) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证参数
            if (longitude == null || latitude == null) {
                response.put("success", false);
                response.put("message", "经纬度参数不能为空");
                return response;
            }

            // 检查 API Key
            if (amapWebKey == null || amapWebKey.trim().isEmpty()) {
                System.err.println("❌ 高德地图 Web API Key 未配置！");
                response.put("success", false);
                response.put("message", "高德地图 API Key 未配置，请在 application-dev.yml 中配置 amap.web-api-key");
                return response;
            }

            System.out.println("📍 开始逆地理编码：经度=" + longitude + ", 纬度=" + latitude);
            System.out.println("🔑 使用 API Key 前缀：" + amapWebKey.substring(0, Math.min(8, amapWebKey.length())) + "...");

            // 构建请求 URL
            String location = longitude + "," + latitude;
            String requestUrl = String.format("%s?location=%s&key=%s&radius=%d&extensions=%s",
                    REGEO_URL, location, amapWebKey, radius, extensions);

            // 调用高德地图 API
            RestTemplate restTemplate = new RestTemplate();
            @SuppressWarnings("unchecked")
            Map<String, Object> amapResponse = restTemplate.getForObject(requestUrl, Map.class);

            System.out.println("📥 高德地图返回数据：" + amapResponse);

            // 检查高德返回状态
            String status = (String) amapResponse.get("status");
            if ("1".equals(status)) {
                // 成功
                @SuppressWarnings("unchecked")
                Map<String, Object> regeocode = (Map<String, Object>) amapResponse.get("regeocode");
                
                if (regeocode != null) {
                    String formattedAddress = (String) regeocode.get("formatted_address");
                    
                    System.out.println("✅ 逆地理编码成功！");
                    System.out.println("📍 格式化地址：" + formattedAddress);
                    
                    response.put("success", true);
                    response.put("formattedAddress", formattedAddress);
                    response.put("regeocode", regeocode);
                    response.put("message", "逆地理编码成功");
                } else {
                    response.put("success", false);
                    response.put("message", "未获取到地址信息");
                }
            } else {
                // 失败
                String infocode = (String) amapResponse.get("infocode");
                String info = (String) amapResponse.get("info");
                
                System.err.println("❌ 高德地图返回错误：" + info);
                
                response.put("success", false);
                response.put("message", "逆地理编码失败：" + info);
                response.put("infocode", infocode);
            }
            
        } catch (Exception e) {
            System.err.println("❌ 逆地理编码异常：" + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "系统错误：" + e.getMessage());
        }
        
        return response;
    }

    /**
     * 批量逆地理编码
     * @param locations 经纬度列表（格式：[[lng1,lat1],[lng2,lat2]]）
     * @return 地址信息列表
     */
    @PostMapping("/regeo/batch")
    public Map<String, Object> batchReverseGeocode(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        // TODO: 实现批量逆地理编码
        response.put("success", false);
        response.put("message", "批量逆地理编码功能开发中");
        
        return response;
    }

    /**
     * 测试接口：检查 API Key 配置
     * @return API Key 状态
     */
    @GetMapping("/test")
    public Map<String, Object> testApiKey() {
        Map<String, Object> response = new HashMap<>();
        
        if (amapWebKey == null || amapWebKey.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "高德地图 Web API Key 未配置");
            response.put("hint", "请在 application-dev.yml 中配置 amap.web-api-key");
        } else {
            response.put("success", true);
            response.put("message", "API Key 已配置");
            response.put("keyPrefix", amapWebKey.substring(0, Math.min(8, amapWebKey.length())) + "...");
        }
        
        return response;
    }
}


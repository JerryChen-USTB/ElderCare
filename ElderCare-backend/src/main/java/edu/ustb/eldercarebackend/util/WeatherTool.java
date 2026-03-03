package edu.ustb.eldercarebackend.util;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 天气查询工具类 - 为LangChain4j提供天气查询Tools功能
 * 帮助老年人用户查询当前天气信息
 */
@Component
public class WeatherTool {

    private static final String WEATHER_API_URL = "https://api.map.baidu.com/weather/v1/?";
    @Value("${baidu.weather.api-key:${BAIDU_WEATHER_API_KEY:}}")
    private String apiKey;
    
    // 存储区县名称到district_id的映射
    private Map<String, String> districtMap = new HashMap<>();
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 应用启动时加载区县数据
     */
    @PostConstruct
    public void loadDistrictData() {
        try {
            System.out.println("🌤️ 开始加载区县数据...");
            InputStream inputStream = getClass().getResourceAsStream("/weather_district_id.csv");
            
            if (inputStream == null) {
                System.err.println("❌ 无法找到weather_district_id.csv文件");
                return;
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            int count = 0;
            
            // 跳过标题行
            reader.readLine();
            
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 5) {  // 确保有足够的列
                    String districtId = parts[0].trim();  // district_id列
                    String district = parts[4].trim();    // district列（区县名）
                    
                    // 去除引号
                    district = district.replaceAll("\"", "");
                    districtId = districtId.replaceAll("\"", "");
                    
                    districtMap.put(district, districtId);
                    count++;
                }
            }
            
            reader.close();
            System.out.println("✅ 区县数据加载完成，共加载 " + count + " 条记录");
            
            // 添加调试输出，确认关键城市是否加载成功
            String[] testCities = {"北京", "上海", "朝阳", "海淀"};
            for (String city : testCities) {
                if (districtMap.containsKey(city)) {
                    System.out.println("🎯 测试城市「" + city + "」 -> district_id: " + districtMap.get(city));
                } else {
                    System.out.println("❌ 测试城市「" + city + "」未找到");
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ 加载区县数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 查询天气信息
     *
     * @param location 区县名称，例如"朝阳区"、"海淀"、"北京"等
     * @param queryType 查询类型："now"表示当前天气，"forecast"表示未来7天天气预报
     * @return 天气查询结果描述
     */
//    @Tool(name = "Weather_query", value = "当用户要求查询天气信息时，必须调用此工具")
    @Tool(name = "天气查询工具", value = "当用户要求查询天气信息时，必须调用此工具")
    public String queryWeather(
            @P(value = "要查询天气的地区名称，例如'朝阳区'、'海淀区'、'北京'等，必须用中文传入", required = true) String location,
            @P(value = "查询类型：'now'表示当前天气，'forecast'表示未来7天天气预报", required = true) String queryType) {
        
        try {
            System.out.println("🌤️ WeatherTool.queryWeather被调用，查询地区: " + location + "，查询类型: " + queryType);
            
            if (!StringUtils.hasText(location)) {
                return "❌ 查询天气失败：请提供要查询的地区名称";
            }
            
            if (!StringUtils.hasText(queryType)) {
                return "❌ 查询天气失败：请提供查询类型（'now'或'forecast'）";
            }
            
            // 验证并转换查询类型
            String dataType;
            if ("now".equals(queryType.toLowerCase())) {
                dataType = "now";
            } else if ("forecast".equals(queryType.toLowerCase())) {
                dataType = "fc";
            } else {
                return "❌ 查询天气失败：查询类型无效，请使用'now'（当前天气）或'forecast'（未来7天预报）";
            }
            
            // 查找district_id
            if (!StringUtils.hasText(apiKey)) {
                return "天气查询失败：未配置百度天气 API Key，请检查 baidu.weather.api-key 或 BAIDU_WEATHER_API_KEY";
            }
            
            String districtId = findDistrictId(location.trim());
            if (districtId == null) {
                return "❌ 查询天气失败：未找到「" + location + "」对应的地区信息，请检查地区名称是否正确";
            }
            
            System.out.println("✅ 找到district_id: " + districtId + " for " + location);
            
            // 调用天气API
            String weatherData = callWeatherAPI(districtId, dataType);
            if (weatherData == null) {
                return "❌ 查询天气失败：天气服务暂时不可用，请稍后再试";
            }
            
            // 解析并格式化天气数据
            return parseWeatherData(weatherData, location, queryType);
            
        } catch (Exception e) {
            System.err.println("天气查询工具调用失败: " + e.getMessage());
            e.printStackTrace();
            return "❌ 查询天气失败：系统出现异常，请稍后再试";
        }
    }
    
    /**
     * 根据地区名称查找district_id
     * 支持模糊匹配：参数包含district字段或district字段包含参数
     */
    private String findDistrictId(String location) {
        System.out.println("🔍 开始查找district_id，输入地区：「" + location + "」");
        
        // 移除常见的行政单位后缀
        String cleanLocation = location.replaceAll("[市区县]$", "");
        System.out.println("🔍 清理后的地区名：「" + cleanLocation + "」");
        
        // 1. 精确匹配原始输入
        if (districtMap.containsKey(location)) {
            System.out.println("✅ 精确匹配成功：「" + location + "」-> " + districtMap.get(location));
            return districtMap.get(location);
        }
        
        // 2. 精确匹配清理后的输入
        if (districtMap.containsKey(cleanLocation)) {
            System.out.println("✅ 清理后精确匹配成功：「" + cleanLocation + "」-> " + districtMap.get(cleanLocation));
            return districtMap.get(cleanLocation);
        }
        
        // 3. 模糊匹配 - 用户输入包含district字段
        for (Map.Entry<String, String> entry : districtMap.entrySet()) {
            String district = entry.getKey();
            if (location.contains(district) && !district.isEmpty()) {
                System.out.println("🎯 模糊匹配成功：用户输入「" + location + "」包含区县「" + district + "」-> " + entry.getValue());
                return entry.getValue();
            }
        }
        
        // 4. 模糊匹配 - district字段包含用户输入
        for (Map.Entry<String, String> entry : districtMap.entrySet()) {
            String district = entry.getKey();
            if (district.contains(cleanLocation) && !cleanLocation.isEmpty()) {
                System.out.println("🎯 模糊匹配成功：区县「" + district + "」包含用户输入「" + cleanLocation + "」-> " + entry.getValue());
                return entry.getValue();
            }
        }
        
        System.out.println("❌ 未找到匹配的区县：" + location + "，数据库共有 " + districtMap.size() + " 条记录");
        return null;
    }
    
    /**
     * 调用百度地图天气API
     */
    private String callWeatherAPI(String districtId, String dataType) {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("district_id", districtId);
            params.put("data_type", dataType);
            params.put("ak", apiKey);
            
            // 构建请求URL
            StringBuilder queryString = new StringBuilder(WEATHER_API_URL);
            for (Map.Entry<String, String> entry : params.entrySet()) {
                queryString.append(entry.getKey()).append("=");
                queryString.append(UriUtils.encode(entry.getValue(), "UTF-8")).append("&");
            }
            
            if (queryString.length() > 0) {
                queryString.deleteCharAt(queryString.length() - 1);
            }
            
            System.out.println("📡 调用天气API: " + queryString.toString());
            
            URL url = new URL(queryString.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000); // 10秒连接超时
            connection.setReadTimeout(10000); // 10秒读取超时
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                System.err.println("❌ API调用失败，响应码: " + responseCode);
                return null;
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            
            reader.close();
            connection.disconnect();
            
            String result = response.toString();
            System.out.println("✅ API响应: " + result);
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ 调用天气API异常: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 解析天气数据并格式化为用户友好的文本
     */
    private String parseWeatherData(String weatherData, String location, String queryType) {
        try {
            JsonNode root = objectMapper.readTree(weatherData);
            
            // 检查API响应状态
            int status = root.get("status").asInt();
            if (status != 0) {
                String message = root.has("message") ? root.get("message").asText() : "未知错误";
                System.err.println("❌ 天气API返回错误: status=" + status + ", message=" + message);
                return "❌ 查询天气失败：" + message;
            }
            
            JsonNode result = root.get("result");
            JsonNode locationInfo = result.get("location");
            String district = locationInfo.get("name").asText();
            
            if ("now".equals(queryType.toLowerCase())) {
                // 解析当前天气数据
                return parseCurrentWeatherData(result, district);
            } else if ("forecast".equals(queryType.toLowerCase())) {
                // 解析未来天气预报数据
                return parseForecastWeatherData(result, district);
            } else {
                return "❌ 查询天气失败：未知的查询类型";
            }
            
        } catch (Exception e) {
            System.err.println("❌ 解析天气数据失败: " + e.getMessage());
            e.printStackTrace();
            return "❌ 查询天气失败：数据解析异常";
        }
    }
    
    /**
     * 解析当前天气数据
     */
    private String parseCurrentWeatherData(JsonNode result, String district) {
        try {
            JsonNode now = result.get("now");
            
            String weather = now.get("text").asText();
            int temperature = now.get("temp").asInt();
            String windClass = now.get("wind_class").asText();
            String windDir = now.get("wind_dir").asText();
            int humidity = now.get("rh").asInt();
            int aqi = now.get("aqi").asInt();
            
            // 格式化回复
            StringBuilder reply = new StringBuilder();
            reply.append("🌤️ ").append(district).append("当前天气：\n\n");
            
            reply.append("🌡️ 天气状况：").append(weather).append("\n");
            reply.append("🌡️ 当前温度：").append(temperature).append("℃\n");
            reply.append("💨 风力风向：").append(windDir).append(" ").append(windClass).append("\n");
            reply.append("💧 空气湿度：").append(humidity).append("%\n");
            reply.append("🍃 空气质量：AQI ").append(aqi);
            
            // 根据AQI给出空气质量等级
            if (aqi <= 50) {
                reply.append("（优）");
            } else if (aqi <= 100) {
                reply.append("（良）");
            } else if (aqi <= 150) {
                reply.append("（轻度污染）");
            } else if (aqi <= 200) {
                reply.append("（中度污染）");
            } else if (aqi <= 300) {
                reply.append("（重度污染）");
            } else {
                reply.append("（严重污染）");
            }
            
            // 添加温馨提醒
            reply.append("\n\n💡 温馨提醒：");
            if (temperature < 10) {
                reply.append("天气较冷，请注意保暖，外出时多穿衣服。");
            } else if (temperature > 30) {
                reply.append("天气较热，请注意防暑降温，多喝水。");
            } else {
                reply.append("天气适宜，是外出活动的好时机。");
            }
            
            if (aqi > 100) {
                reply.append("空气质量较差，建议减少户外活动。");
            }
            
            return reply.toString();
            
        } catch (Exception e) {
            System.err.println("❌ 解析当前天气数据失败: " + e.getMessage());
            return "❌ 查询当前天气失败：数据解析异常";
        }
    }
    
    /**
     * 解析未来天气预报数据
     */
    private String parseForecastWeatherData(JsonNode result, String district) {
        try {
            JsonNode forecasts = result.get("forecasts");
            
            if (forecasts == null || !forecasts.isArray()) {
                return "❌ 查询未来天气失败：未获取到天气预报数据";
            }
            
            StringBuilder reply = new StringBuilder();
            reply.append("🌤️ ").append(district).append("未来7天天气预报：\n\n");
            
            for (int i = 0; i < forecasts.size() && i < 7; i++) {
                JsonNode forecast = forecasts.get(i);
                
                String date = forecast.get("date").asText();
                String week = forecast.get("week").asText();
                String textDay = forecast.get("text_day").asText();
                String textNight = forecast.get("text_night").asText();
                int high = forecast.get("high").asInt();
                int low = forecast.get("low").asInt();
                String wdDay = forecast.get("wd_day").asText();
                String wcDay = forecast.get("wc_day").asText();
                
                // 格式化每日天气信息
                reply.append("📅 ").append(date).append(" ").append(week).append("\n");
                reply.append("🌤️ 白天：").append(textDay).append("，夜间：").append(textNight).append("\n");
                reply.append("🌡️ 气温：").append(low).append("℃ ~ ").append(high).append("℃\n");
                reply.append("💨 风向风力：").append(wdDay).append(" ").append(wcDay).append("\n");
                
                if (i < forecasts.size() - 1) {
                    reply.append("\n");
                }
            }
            
            // 添加温馨提醒
            reply.append("\n💡 温馨提醒：");
            
            // 分析未来几天的天气趋势
            JsonNode firstDay = forecasts.get(0);
            JsonNode lastDay = forecasts.get(Math.min(forecasts.size() - 1, 6));
            
            int firstTemp = (firstDay.get("high").asInt() + firstDay.get("low").asInt()) / 2;
            int lastTemp = (lastDay.get("high").asInt() + lastDay.get("low").asInt()) / 2;
            
            if (lastTemp - firstTemp > 5) {
                reply.append("未来几天气温将逐渐升高，请适时调整衣物。");
            } else if (firstTemp - lastTemp > 5) {
                reply.append("未来几天气温将逐渐降低，请注意保暖。");
            } else {
                reply.append("未来几天气温较为稳定，请根据天气情况合理安排出行。");
            }
            
            return reply.toString();
            
        } catch (Exception e) {
            System.err.println("❌ 解析未来天气数据失败: " + e.getMessage());
            return "❌ 查询未来天气失败：数据解析异常";
        }
    }
}

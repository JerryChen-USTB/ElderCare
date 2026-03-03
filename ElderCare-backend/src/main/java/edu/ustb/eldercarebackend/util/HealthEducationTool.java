package edu.ustb.eldercarebackend.util;

import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.ustb.eldercarebackend.config.BaiduLingyiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 健康科普工具类 - 调用百度智能云灵医开放平台API
 * 为用户提供权威的健康医疗知识科普
 */
@Component
public class HealthEducationTool {

    @Autowired
    private BaiduLingyiProperties lingyiProperties;

    /**
     * 健康科普查询工具
     * 调用百度智能云灵医开放平台提供权威的健康医疗知识
     *
     * @param question 用户的健康医疗相关问题
     * @return 权威的健康科普回答
     */
    @Tool(name = "健康科普工具", value = "当用户询问健康、医疗、疾病、药物、症状等相关问题时，如果知识库中没有提供相关信息，则调用此工具")
//    @Tool(name = "Health_literacy", value = "当用户询问健康、医疗、疾病、药物、症状等相关问题时，如果知识库中没有提供相关信息，则调用此工具")
    public String getHealthEducation(
            @P(value = "用户的健康医疗相关问题，例如：为什么喝牛奶之后拉肚子？", required = true) String question) {
        
        try {
            System.out.println("🏥 调用健康科普工具，问题: " + question);
            
            // 构建请求体
            MessageBean messageBean = new MessageBean();
            messageBean.setModel(lingyiProperties.getModel());
            messageBean.setStream(false);
            
            Message message = new Message();
            message.setRole("user");
            message.setCreated(System.currentTimeMillis() / 1000); // 设置当前时间戳
            message.setVersion("api-v2");
            
            Content content = new Content();
            content.setType("text");
            content.setBody(question); // 设置用户问题
            
            message.setContent(Arrays.asList(content));
            messageBean.setMessages(Arrays.asList(message));
            
            // 调用API获取回答
            String response = callLingyiAPI(messageBean);
            
            // 解析响应
            String result = parseResponse(response);
            
            System.out.println("✅ 健康科普工具调用成功");
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ 健康科普工具调用失败: " + e.getMessage());
            e.printStackTrace();
            return "抱歉，当前无法获取健康科普信息，请稍后再试。";
        }
    }

    /**
     * 调用百度智能云灵医开放平台API
     */
    private String callLingyiAPI(MessageBean messageBean) throws Exception {
        String messageJson = JSONObject.toJSONString(messageBean);
        messageJson = string2Unicode(messageJson);
        
        String md5 = getMd5(messageJson);
        DateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        String trimester = sdf.format(new Date());
        String authStringPrefix = "ihcloud/" + lingyiProperties.getAk() + "/" + trimester + "/300";
        String signingKey = hmacSha256(lingyiProperties.getSk(), authStringPrefix);
        
        String path = "/api/01bot/sse-gateway/stream";
        String url = lingyiProperties.getHost() + path;
        URL obj = URI.create(url).toURL();
        String canonicalRequest = String.join("\n", "POST", obj.getPath(), "content-md5:" + md5);
        String signature = hmacSha256(signingKey, canonicalRequest);
        
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("X-IHU-Authorization-V2", authStringPrefix + "/" + signature);
        con.setDoOutput(true);
        
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(messageJson);
        wr.flush();
        wr.close();
        
        int responseCode = con.getResponseCode();
        System.out.println("📡 API响应状态码: " + responseCode);
        
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        
        return response.toString();
    }

    /**
     * 解析API响应，提取健康科普内容
     */
    private String parseResponse(String response) {
        try {
            JSONObject jsonResponse = JSONObject.parseObject(response);
            
            // 检查错误码
            int errorCode = jsonResponse.getIntValue("error_code");
            if (errorCode != 0) {
                String errorMsg = jsonResponse.getString("error_msg");
                System.err.println("❌ API返回错误: " + errorCode + " - " + errorMsg);
                return "抱歉，获取健康科普信息时出现错误，请稍后再试。";
            }
            
            // 提取回答内容
            if (jsonResponse.containsKey("result") && jsonResponse.getJSONArray("result").size() > 0) {
                JSONObject result = jsonResponse.getJSONArray("result").getJSONObject(0);
                if (result.containsKey("messages") && result.getJSONArray("messages").size() > 0) {
                    JSONObject message = result.getJSONArray("messages").getJSONObject(0);
                    if (message.containsKey("content") && message.getJSONArray("content").size() > 0) {
                        JSONObject content = message.getJSONArray("content").getJSONObject(0);
                        if (content.containsKey("body")) {
                            String body = content.getString("body");
                            
                            // 直接返回灵医平台的原始回答，不添加额外格式
                            // 大模型会根据SystemMessage的要求进行格式处理和来源标注
                            return body;
                        }
                    }
                }
            }
            
            return "抱歉，未能获取到有效的健康科普信息。";
            
        } catch (Exception e) {
            System.err.println("❌ 解析API响应失败: " + e.getMessage());
            return "抱歉，解析健康科普信息时出现错误。";
        }
    }

    /**
     * 计算MD5哈希
     */
    private String getMd5(String data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 字符串转Unicode编码
     */
    private String string2Unicode(String string) {
        if (string.equals("")) {
            return null;
        }
        char[] bytes = string.toCharArray();
        StringBuilder unicode = new StringBuilder();
        for (char c : bytes) {
            // 标准ASCII范围内的字符，直接输出
            if (c >= 0 && c <= 127) {
                unicode.append(c);
                continue;
            }
            String hexString = Integer.toHexString(c);
            unicode.append("\\u");
            // 不够四位进行补0操作
            if (hexString.length() < 4) {
                unicode.append("0000".substring(hexString.length(), 4));
            }
            unicode.append(hexString);
        }
        return unicode.toString();
    }

    /**
     * HMAC-SHA256加密
     */
    private String hmacSha256(String secret, String message) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKeySpec);
        byte[] rawHmac = mac.doFinal(message.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : rawHmac) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // 内部类定义，用于构建请求体
    @SuppressWarnings("unused") // 这些getter方法是Jackson序列化所需要的
    private static class MessageBean {
        private String model;
        private boolean stream;
        private List<Message> messages;

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public boolean isStream() { return stream; }
        public void setStream(boolean stream) { this.stream = stream; }
        public List<Message> getMessages() { return messages; }
        public void setMessages(List<Message> messages) { this.messages = messages; }
    }

    @SuppressWarnings("unused") // 这些getter方法是Jackson序列化所需要的
    private static class Message {
        private String version;
        private long created;
        private String role;
        private List<Content> content;

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public long getCreated() { return created; }
        public void setCreated(long created) { this.created = created; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public List<Content> getContent() { return content; }
        public void setContent(List<Content> content) { this.content = content; }
    }

    @SuppressWarnings("unused") // 这些getter方法是Jackson序列化所需要的
    private static class Content {
        private String type;
        private String body;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
    }
}

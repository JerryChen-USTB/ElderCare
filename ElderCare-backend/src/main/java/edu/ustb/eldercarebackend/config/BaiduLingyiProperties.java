package edu.ustb.eldercarebackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 百度智能云灵医开放平台配置属性
 */
@Component
@ConfigurationProperties(prefix = "baidu.lingyi")
public class BaiduLingyiProperties {
    
    private String ak;
    private String sk;
    private String host;
    private String model;

    public String getAk() {
        return ak;
    }

    public void setAk(String ak) {
        this.ak = ak;
    }

    public String getSk() {
        return sk;
    }

    public void setSk(String sk) {
        this.sk = sk;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}

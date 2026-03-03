/**
 * 项目配置文件
 * 支持多环境配置和便捷切换
 */

// 配置选项
const configs = {
  // 开发环境配置
  development: {
    // 浏览器开发环境
    h5: 'http://localhost:8080',
    // 手机APP开发环境（需要替换为您的IP地址）
    // app: 'http://192.168.0.110:8080',
    // app: 'http://www.ginkgo-eldercare.top',
    app: 'http://192.168.0.105:8080',
	// app: 'http://192.168.1.10:8080',
    // 小程序开发环境
    mp: 'http://192.168.0.110:8080'
  },
  
  // 生产环境配置
  production: {
    // 生产环境API地址
    h5: 'https://your-domain.com',
    app: 'http://www.ginkgo-eldercare.top',
    mp: 'https://your-domain.com'
  }
};

// 当前环境（development 或 production）
const ENV = 'production';

/**
 * 自动获取当前平台的API地址
 */
const getApiUrl = () => {
  const config = configs[ENV];
  
  // #ifdef H5
  console.log('检测到浏览器环境，使用:', config.h5);
  return config.h5;
  // #endif
  
  // #ifdef APP-PLUS
  console.log('检测到APP环境，使用:', config.app);
  return config.app;
  // #endif
  
  // #ifdef MP-WEIXIN
  console.log('检测到微信小程序环境，使用:', config.mp);
  return config.mp;
  // #endif
  
  // 默认使用H5配置
  console.log('使用默认配置:', config.h5);
  return config.h5;
};

/**
 * 手动设置IP地址（便于快速切换）
 * @param {string} ip - 新的IP地址
 */
const setDevIpAddress = (ip) => {
  configs.development.app = `http://${ip}:8080`;
  configs.development.mp = `http://${ip}:8080`;
  console.log('IP地址已更新为:', ip);
};

/**
 * 获取当前使用的API地址
 */
const API_BASE_URL = getApiUrl();

export default {
  API_BASE_URL,
  getApiUrl,
  setDevIpAddress,
  ENV,
  configs
};

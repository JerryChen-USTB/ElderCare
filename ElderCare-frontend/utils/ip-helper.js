/**
 * IP地址快速切换工具
 * 使用方法：在浏览器控制台执行 switchIp('新IP地址')
 */

import config from './config.js';

/**
 * 快速切换IP地址
 * @param {string} newIp - 新的IP地址
 */
const switchIp = (newIp) => {
  if (!newIp) {
    console.error('请提供有效的IP地址');
    return;
  }
  
  // 验证IP地址格式
  const ipRegex = /^(\d{1,3}\.){3}\d{1,3}$/;
  if (!ipRegex.test(newIp)) {
    console.error('IP地址格式不正确，请使用类似 192.168.1.100 的格式');
    return;
  }
  
  config.setDevIpAddress(newIp);
  
  console.log(`✅ IP地址已切换为: ${newIp}`);
  console.log('🔄 请刷新页面或重新启动应用以使配置生效');
};

/**
 * 显示当前配置
 */
const showConfig = () => {
  console.log('📱 当前环境配置:');
  console.log('浏览器(H5):', config.configs.development.h5);
  console.log('手机APP:', config.configs.development.app);
  console.log('小程序:', config.configs.development.mp);
  console.log('当前使用:', config.API_BASE_URL);
};

/**
 * 获取本机IP地址提示
 */
const getIpTips = () => {
  console.log('💡 如何获取本机IP地址:');
  console.log('Windows: 在CMD中执行 ipconfig');
  console.log('Mac/Linux: 在终端中执行 ifconfig');
  console.log('然后找到 IPv4 地址，通常是 192.168.x.x 格式');
  console.log('');
  console.log('🔧 使用方法:');
  console.log('switchIp("192.168.1.100")  // 替换为您的实际IP');
};

// 将函数挂载到全局，方便控制台调用
if (typeof window !== 'undefined') {
  window.switchIp = switchIp;
  window.showConfig = showConfig;
  window.getIpTips = getIpTips;
}

export default {
  switchIp,
  showConfig,
  getIpTips
};

import LibGenerateTestUserSig from './lib-generate-test-usersig-es.min.js';
import request from '@/utils/request.js';

/**
 * 签名过期时间，建议不要设置的过短
 * <p>
 * 时间单位：秒
 * 默认时间：7 x 24 x 60 x 60 = 604800 = 7 天
 */
const EXPIRETIME = 604800;

/**
 * TRTC配置缓存
 * 从后端获取配置后缓存，避免频繁请求
 */
let trtcConfig = null;

/**
 * 从后端获取TRTC配置
 * 避免敏感信息硬编码在前端
 */
async function getTrtcConfig() {
  // 如果已有缓存，直接返回
  if (trtcConfig) {
    console.log('📦 使用缓存的TRTC配置:', trtcConfig);
    return trtcConfig;
  }
  
  try {
    console.log('🌐 正在从后端获取TRTC配置...');
    const response = await request.request({
      url: '/trtc/config',
      method: 'GET'
    });
    
    console.log('📡 后端响应:', response);
    
    if (response && response.success) {
      // 确保 sdkAppId 是数字类型
      trtcConfig = {
        sdkAppId: Number(response.sdkAppId),
        secretKey: response.secretKey
      };
      console.log('✅ TRTC配置获取成功:', {
        sdkAppId: trtcConfig.sdkAppId,
        secretKeyLength: trtcConfig.secretKey ? trtcConfig.secretKey.length : 0
      });
      return trtcConfig;
    } else {
      console.error('❌ 后端返回失败:', response);
      throw new Error('获取TRTC配置失败: ' + (response?.message || '未知错误'));
    }
  } catch (error) {
    console.error('❌ 获取TRTC配置异常:', error);
    throw error;
  }
}

/*
 * Module:   GenerateTestUserSig
 *
 * Function: 用于生成测试用的 UserSig，UserSig 是腾讯云为其云服务设计的一种安全保护签名。
 *           其计算方法是对 SDKAppID、UserID 和 EXPIRETIME 进行加密，加密算法为 HMAC-SHA256。
 *
 * Attention: 本方案已优化：配置信息从后端服务器获取，而非硬编码在前端。
 *            这样可以更好地保护您的加密密钥，避免密钥泄露导致的流量盗用。
 *
 * Reference：https://cloud.tencent.com/document/product/647/17275#Server
 */
/**
 * 诊断函数：测试TRTC配置获取
 * 用于排查问题
 */
export async function testTrtcConfig() {
  try {
    console.log('🔍 开始诊断TRTC配置...');
    const config = await getTrtcConfig();
    console.log('📋 诊断结果:');
    console.log('  - sdkAppId:', config.sdkAppId, '(类型:', typeof config.sdkAppId, ')');
    console.log('  - secretKey:', config.secretKey ? '已设置 (长度:' + config.secretKey.length + ')' : '未设置');
    
    if (!config.sdkAppId) {
      console.error('❌ sdkAppId 为空或未定义');
      return false;
    }
    
    if (!config.secretKey) {
      console.error('❌ secretKey 为空或未定义');
      return false;
    }
    
    console.log('✅ TRTC配置诊断通过');
    return true;
  } catch (error) {
    console.error('❌ TRTC配置诊断失败:', error);
    return false;
  }
}

export async function genTestUserSig(userID) {
  try {
    console.log('🔑 开始生成UserSig，userID:', userID);
    
    // 从后端获取TRTC配置
    const config = await getTrtcConfig();
    
    if (!config || !config.sdkAppId || !config.secretKey) {
      throw new Error('TRTC配置不完整');
    }
    
    console.log('🎯 使用配置生成签名:', {
      sdkAppId: config.sdkAppId,
      userID: userID,
      expireTime: EXPIRETIME
    });
    
    const generator = new LibGenerateTestUserSig(config.sdkAppId, config.secretKey, EXPIRETIME);
    const userSig = generator.genTestUserSig(userID);

    const result = {
      sdkAppID: config.sdkAppId,
      userSig,
    };
    
    console.log('✅ UserSig生成成功:', {
      sdkAppID: result.sdkAppID,
      userSigLength: result.userSig ? result.userSig.length : 0
    });
    
    return result;
  } catch (error) {
    console.error('❌ 生成UserSig失败:', error);
    throw error;
  }
}
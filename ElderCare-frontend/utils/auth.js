/**
 * 用户认证相关工具方法
 */

/**
 * 获取当前登录用户的ID
 * @returns {number|null} 用户ID，如果未登录返回null
 */
export const getCurrentUserId = () => {
  try {
    const userInfo = uni.getStorageSync('userInfo');
    if (userInfo && userInfo.id) {
      return userInfo.id;
    }
    return null;
  } catch (error) {
    console.error('获取用户ID失败:', error);
    return null;
  }
};

/**
 * 获取当前登录用户的完整信息
 * @returns {object|null} 用户信息对象，如果未登录返回null
 */
export const getCurrentUserInfo = () => {
  try {
    const userInfo = uni.getStorageSync('userInfo');
    if (userInfo) {
      return userInfo;
    }
    return null;
  } catch (error) {
    console.error('获取用户信息失败:', error);
    return null;
  }
};

/**
 * 检查用户是否已登录
 * @returns {boolean} 是否已登录
 */
export const isLoggedIn = () => {
  const userInfo = getCurrentUserInfo();
  return userInfo !== null && userInfo.id !== undefined;
};

/**
 * 获取当前用户的角色
 * @returns {string|null} 用户角色，如果未登录返回null
 */
export const getCurrentUserRole = () => {
  const userInfo = getCurrentUserInfo();
  return userInfo ? userInfo.role : null;
};

/**
 * 清除用户登录信息
 */
export const clearUserInfo = () => {
  try {
    uni.removeStorageSync('userInfo');
    console.log('用户信息已清除');
  } catch (error) {
    console.error('清除用户信息失败:', error);
  }
};

/**
 * 检查是否需要登录，如果未登录则跳转到登录页
 * @param {boolean} showToast 是否显示提示
 * @returns {boolean} 如果已登录返回true，未登录返回false
 */
export const requireLogin = (showToast = true) => {
  if (!isLoggedIn()) {
    if (showToast) {
      uni.showToast({
        title: '请先登录',
        icon: 'none'
      });
    }
    
    setTimeout(() => {
      uni.navigateTo({
        url: '/pages/login/login'
      });
    }, 1500);
    
    return false;
  }
  return true;
};
// 请求基础配置 - 使用统一配置管理
import config from './config.js';

const BASE_URL = config.API_BASE_URL;

// 打印当前使用的API地址（便于调试）
console.log('📡 当前API地址:', BASE_URL);

/**
 * 封装网络请求
 */
const request = (options) => {
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json;charset=UTF-8',
        ...options.header
      },
      success: (res) => {
        if (res.statusCode === 200) {
          resolve(res.data);
        } else {
          reject(res);
        }
      },
      fail: (error) => {
        console.error('请求失败:', error);
        uni.showToast({
          title: '网络请求失败',
          icon: 'none'
        });
        reject(error);
      }
    });
  });
};

/**
 * GET请求
 */
const get = (url, data, header) => {
  return request({
    url,
    method: 'GET',
    data,
    header
  });
};

/**
 * POST请求
 */
const post = (url, data, header) => {
  return request({
    url,
    method: 'POST',
    data,
    header
  });
};

/**
 * PUT请求
 */
const put = (url, data, header) => {
  return request({
    url,
    method: 'PUT',
    data,
    header
  });
};

/**
 * DELETE请求
 */
const del = (url, data, header) => {
  return request({
    url,
    method: 'DELETE',
    data,
    header
  });
};

// 用户相关API
const userApi = {
  // 用户登录
  login: (phone, password) => {
    return post('/api/user/login', { phone, password });
  },
  
  // 用户注册
  register: (userInfo) => {
    return post('/api/user/register', userInfo);
  },
  
  // 获取用户信息
  getUserInfo: (phone) => {
    return get(`/api/user/info/${phone}`);
  },
  
  // 更新用户角色
  updateRole: (phone, role) => {
    return post('/api/user/updateRole', { phone, role });
  },
  // 【新增】发送忘记密码验证码
    sendForgotPwdCode: (data) => {
      return post('/api/user/forgot-password/send-code', data);
    },
    
    // 【新增】重置密码
    resetPassword: (data) => {
      return post('/api/user/forgot-password/reset', data);
    }
};

// 老人信息相关API
const elderApi = {
  // 获取老人信息
  getElderInfo: (userId) => {
    return get(`/api/elder/info/${userId}`);
  },
  
  // 更新老人姓名
  updateElderName: (userId, name) => {
    return post('/api/elder/updateName', { userId, name });
  },
  
  // 上传头像
  uploadAvatar: (file, userId) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('userId', userId);
    
    return new Promise((resolve, reject) => {
      uni.uploadFile({
        url: BASE_URL + '/api/elder/uploadAvatar',
        file: file,
        name: 'file',
        formData: {
          userId: userId
        },
        success: (res) => {
          try {
            const data = JSON.parse(res.data);
            resolve(data);
          } catch (e) {
            reject(e);
          }
        },
        fail: (error) => {
          reject(error);
        }
      });
    });
  },
  
  // 更新老人基本信息
  updateElderInfo: (elderInfo) => {
    return post('/api/elder/updateInfo', elderInfo);
  }
};

// 聊天相关API
const chatApi = {
  // 普通聊天（非流式）
  sendMessage: (message, memoryId) => {
    return post('/api/chat/message', { message, memoryId });
  },
  
  // 清理会话记录
  clearChatMemory: (memoryId) => {
    return post('/api/chat/memory/clear', { memoryId });
  },
  
  // 获取会话历史记录
  getChatHistory: (memoryId) => {
    return post('/api/chat/memory/history', { memoryId });
  },
  
  // WebSocket流式聊天（完美支持Android）
  sendWebSocketMessage: (message, onChunk, onComplete, onError, memoryId) => {
    return new Promise((resolve, reject) => {
      console.log('🔌 使用WebSocket流式聊天', message, memoryId);
      
      let fullResponse = '';
      let isCompleted = false;
      let socket = null;
      
      try {
        // 构建WebSocket URL
        const wsUrl = BASE_URL.replace('http://', 'ws://').replace('https://', 'wss://') + '/ws/chat';
        console.log('🔗 WebSocket连接地址:', wsUrl);
        
        // 创建WebSocket连接
        socket = uni.connectSocket({
          url: wsUrl,
          success: () => {
            console.log('✅ WebSocket连接成功');
          },
          fail: (error) => {
            console.error('❌ WebSocket连接失败:', error);
            if (!isCompleted) {
              isCompleted = true;
              if (onError) onError(error);
              reject(error);
            }
          }
        });
        
        // 监听WebSocket连接打开
        socket.onOpen(() => {
          console.log('🔌 WebSocket连接已建立');
          
          // 发送聊天消息
          const request = {
            action: 'chat',
            message: message,
            memoryId: memoryId || 'default-session'
          };
          
          socket.send({
            data: JSON.stringify(request),
            success: () => {
              console.log('📤 发送WebSocket消息成功');
            },
            fail: (error) => {
              console.error('❌ 发送WebSocket消息失败:', error);
              if (!isCompleted) {
                isCompleted = true;
                socket.close();
                if (onError) onError(error);
                reject(error);
              }
            }
          });
        });
        
        // 监听WebSocket消息
        socket.onMessage((res) => {
          try {
            const response = JSON.parse(res.data);
            
            switch (response.type) {
              case 'connected':
                break;
                
              case 'start':
                break;
                
              case 'chunk':
                // 流式数据块
                const chunk = response.data;
                fullResponse += chunk;
                if (onChunk) onChunk(fullResponse);
                break;
                
              case 'complete':
                // 聊天完成
                if (!isCompleted) {
                  isCompleted = true;
                  socket.close();
                  if (onComplete) onComplete(fullResponse);
                  resolve(fullResponse);
                }
                break;
                
              case 'fallback':
                // 网络错误自动降级
                console.log('🔄 收到降级信号，自动切换到普通模式:', response.message);
                if (!isCompleted) {
                  isCompleted = true;
                  socket.close();
                  // 直接触发降级处理，而不是报错
                  if (onError) onError(new Error('network_fallback'));
                  reject(new Error('network_fallback'));
                }
                break;
                
              case 'error':
                // 错误处理
                console.error('❌ WebSocket服务器错误:', response.message);
                if (!isCompleted) {
                  isCompleted = true;
                  socket.close();
                  if (onError) onError(new Error(response.message));
                  reject(new Error(response.message));
                }
                break;
                
              default:
                console.warn('⚠️ 未知的WebSocket消息类型:', response.type);
            }
            
          } catch (parseError) {
            console.error('❌ 解析WebSocket消息失败:', parseError, res.data);
          }
        });
        
        // 监听WebSocket连接关闭
        socket.onClose((res) => {
          console.log('🔌 WebSocket连接已关闭:', res);
          if (!isCompleted) {
            // 非正常关闭
            isCompleted = true;
            if (fullResponse.length > 0) {
              // 如果已经有部分响应，则认为成功
              if (onComplete) onComplete(fullResponse);
              resolve(fullResponse);
            } else {
              // 没有响应数据，认为是错误
              if (onError) onError(new Error('WebSocket连接意外关闭'));
              reject(new Error('WebSocket连接意外关闭'));
            }
          }
        });
        
        // 监听WebSocket错误
        socket.onError((error) => {
          console.error('❌ WebSocket错误:', error);
          if (!isCompleted) {
            isCompleted = true;
            socket.close();
            if (onError) onError(error);
            reject(error);
          }
        });
        
        // 超时处理
        setTimeout(() => {
          if (!isCompleted) {
            console.warn('⏰ WebSocket连接超时');
            isCompleted = true;
            socket.close();
            if (onError) onError(new Error('连接超时'));
            reject(new Error('连接超时'));
          }
        }, 30000); // 30秒超时
        
      } catch (error) {
        console.error('❌ WebSocket初始化失败:', error);
        if (!isCompleted) {
          isCompleted = true;
          if (socket) socket.close();
          if (onError) onError(error);
          reject(error);
        }
      }
    });
    }
};

// 预约相关API
const appointmentApi = {
  // 创建预约
  createAppointment: (appointmentData) => {
    return post('/api/appointment/create', appointmentData);
  },
  
  // 获取用户的预约列表
  getAppointmentsByElderId: (elderId) => {
    return get(`/api/appointment/list/${elderId}`);
  },
  
  // 根据ID获取预约详情
  getAppointmentById: (appointmentId) => {
    return get(`/api/appointment/${appointmentId}`);
  },
  
  // 取消预约
  cancelAppointment: (appointmentId) => {
    return put(`/api/appointment/cancel/${appointmentId}`);
  },
  
  // 获取待接单的预约列表（供志愿者查看）
  getPendingAppointments: () => {
    return get('/api/appointment/pending');
  }
};

export default {
  request,
  get,
  post,
  put,
  del,
  userApi,
  elderApi,
  chatApi,
  appointmentApi,
  BASE_URL  // 导出BASE_URL供其他文件使用
};
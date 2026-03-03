import CryptoJS from 'crypto-js';
class TengxunAsr {
  constructor({ secretId = '', appId = '', secretKey = '', params = {} } = {}) {
    // 基础的入参
    this.secretId = secretId;
    this.appId = appId;
    this.secretKey = secretKey;
    this.url = 'asr.cloud.tencent.com/asr/v2/';
    // socket对象
    this.socket = null;
    this.type = 'tengxun';
    // asr语音方面的控制参数
    this.params = params;
    this.callBack = {
      openSuccess: () => {},
      openFail: () => {},
      onOpen: () => {},
      onError: () => {},
      onMessage: () => {},
      onClose: () => {}
    };
  }

  // 初始化链接url
  getWebSocketUrl(url, params) {
    let newurl =
      url +
      this.appId +
      '?' +
      Object.keys(params)
        .sort(function (a, b) {
          return a.localeCompare(b);
        })
        .map(key => {
          return encodeURIComponent(key) + '=' + encodeURIComponent(params[key]);
        })
        .join('&');
    let baseurl = this.baseUrl(newurl);
    newurl = newurl + '&' + 'signature' + '=' + encodeURIComponent(baseurl);
    return newurl;
  }
  baseUrl(newurl) {
    const hash = CryptoJS.HmacSHA1(newurl, this.secretKey);
    return uni.arrayBufferToBase64(this.toUint8Array(hash));
  }
  toUint8Array(wordArray) {
    // Shortcuts
    const words = wordArray.words;
    const sigBytes = wordArray.sigBytes;

    // Convert
    const u8 = new Uint8Array(sigBytes);
    for (let i = 0; i < sigBytes; i++) {
      u8[i] = (words[i >>> 2] >>> (24 - (i % 4) * 8)) & 0xff;
    }
    return u8;
  }

  init({ openSuccess = () => {}, openFail = () => {}, onOpen = () => {}, onError = () => {}, onMessage = () => {}, onClose = () => {} } = {}) {
    this.callBack = {
      openSuccess,
      openFail,
      onOpen,
      onError,
      onMessage,
      onClose
    };
  }
  // 修改会变的asr入参
  changeParams(params) {
    this.params = params;
  }
  // 进行连接
  /**
   * openSuccess:启动成回调
   * openFail 启动失败回调
   * onOpen 启动时触发
   * onError 启动失败时触发
   * onMessage 持续发信息数据触发
   * onClose 关闭时候触发
   */
 async open() {
    if (this.socket) {
      console.error('已经存在一个链接');
	  return
    }
    // @ts-ignore
    const timestamp = parseInt(new Date().getTime() / 1000) - 1;
    // 默认不经常改动的参数放在这里，你们可以根据腾讯云文档，自行添加修改
    const params = {
      secretid: this.secretId,
      timestamp: timestamp,
      expired: timestamp + 60 * 60,
      nonce: timestamp,
      engine_model_type: '16k_zh', // 中国话
      voice_id: timestamp.toString(),
      voice_format: 1, // pcm
      convert_num_mode: 0,
      ...this.params
    };
    const url =await this.getWebSocketUrl(this.url, params);
    console.log(url);
    // 初始化socket
    this.socket = uni.connectSocket({
      url: 'wss://' + url,
      success: data => {
        console.log('socket connect result ', data);
        this.callBack.openSuccess();
      },
      fail: e => {
        console.log('%c Line:63 🍺 e', 'color:#ea7e5c', e);
        this.callBack.openFail(e);
      }
    });
     uni.onSocketOpen(res => {
      console.log('WebSocket连接已打开！');
      this.callBack.onOpen(res);
    });
    uni.onSocketError(res => {
      console.log('WebSocket连接打开失败，请检查！', res);
      this.callBack.onError(res);
    });
    uni.onSocketMessage(res => {
      console.log('收到服务器内容', res.data);
      const result = JSON.parse(res.data);

      this.callBack.onMessage(result);
    });
    uni.onSocketClose(res => {
      console.log('WebSocket 已关闭！');
      this.socket = null;
      this.callBack.onClose(res);
    });
  }
  close() {
    this.socket && this.socket.close();
  }
  send(res) {
    if (this.socket) {
      uni.sendSocketMessage(res);
    } else {
      console.log('websocket无链接');
    }
  }
}
export default TengxunAsr;

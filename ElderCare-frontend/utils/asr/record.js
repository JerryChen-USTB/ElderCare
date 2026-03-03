// const recorder = uni.requireNativePlugin('yyds-recorder8000');
const recorder = uni.requireNativePlugin('ZLSD-Recorder'); // 1600采样率用这个
import { base64ToPath, pcmToWavArraybuffer } from './tool';

function checkPlatform() {
  let platform = null;
  switch (uni.getSystemInfoSync().platform) {
    case 'android':
      platform = 'android';
      break;
    case 'ios':
      platform = 'ios';
      break;
    default:
      break;
  }
  return platform;
}

class Record {
  constructor() {
    // 是否在录音
    this.isRecording = false;
    // 开始录音回调函数
    this.startCallback = null;
    // 停止录音回调函数
    this.stopCallback = null;
    // 持续录音过程中持续触发的回调函数，参数需要获取每frameSize大小的音频文件数据
    this.frameRecordedCallback = null;
    // 开始录音时间，用于判断录音时长，限制最大录音时间
    this.startTime = null;
    this.btyArray = [];
    this.createPath = false;
    this.platform = checkPlatform();
    // 分贝数组
    this.dBArray = [];
  }
  // 监听结束
  onStop(callback) {
    if (Object.prototype.toString.call(callback) !== '[object Function]') {
      throw Error('参数不是一个function');
    }
    this.stopCallback = callback;
  }
  // 监听开始
  onStart(callback) {
    if (Object.prototype.toString.call(callback) !== '[object Function]') {
      throw Error('参数不是一个function');
    }
    this.startCallback = callback;
  }
  start({ frameSize = 5, duration = 60 * 1000, sampleRate = 16000, isSingleChannel = true, encodingBit = 16, createPath = false } = {}) {
    console.log('%c Line:50 🌭 录音时长duration', 'color:#93c0a4', duration);
    if (this.isRecording) {
      // 如果已经录音了，还调用start,那么就报错
      throw Error('已经录音中，无法多次start');
    } else {
      // 设置开始录音时间
      this.startTime = +new Date();
      this.btyArray = [];
      this.startCallback && this.startCallback();
      this.isRecording = true;
      this.createPath = createPath;
      // 'frameSize'：int,//帧大小（必须，单位：字节） 1kb=1024字节
      // 'sampleRate':int，采样率（16000采样率插件写死16000）8000采样率插件的要写死8000
      // 'isSingleChannel':boolean，//是否单声道（非必须，默认false）
      // 'encodingBit':int，//编码位数（非必须，取值8/16，默认16）
      // 'duration':number // 默认录音时间
      recorder.start({ frameSize: 1024 * frameSize, sampleRate, isSingleChannel, encodingBit }, ret => {
        // 录音时长超出duration就主动终止
        if (this.startTime && +new Date() - this.startTime > duration) {
          this.stop();
          throw Error(`录音超时，设置录音最长时间为 ${duration} ms`);
        }
        var a;
        // 处理ios的情况
        if (this.platform === 'ios') {
          let array = ret.split(',');
          array = this.strArryToIntArry(array);
          this.btyArray = this.btyArray.concat(array);
          a = this.arrayToArrayBuffer(array) || [];
          // 计算分贝
          this.getDB(a);
          this.frameRecordedCallback &&
            this.frameRecordedCallback({
              frameBuffer: a,
              dBArray: this.isRecording ? this.dBArray : []
            });
          // 处理安卓的情况，因为调用的插件不一样
        } else if (this.platform === 'android') {
          this.btyArray = this.btyArray.concat(ret.data || []);
          a = (ret.data && this.arrayToArrayBuffer(ret.data)) || [];
          let c = ret.error; //string （error与data/size互斥）
          if (c) {
            throw Error('安卓录音报错：',c);
          }
          // 计算分贝
          this.getDB(a);
          this.frameRecordedCallback &&
            this.frameRecordedCallback({
              frameBuffer: a,
              dBArray: this.isRecording ? this.dBArray : [],
              error: c
            });
        }
      });
    }
  }
  //  计算音量
  getDB(buffer) {
    var pcmArr = new Int16Array(buffer);
    var size = pcmArr.length;
    var sum = 0;
    for (var i = 0; i < size; i++) {
      sum += Math.abs(pcmArr[i]);
    }
    let powerLevel = (sum * 500.0) / (size * 16383) / 2;
    if (powerLevel >= 100) {
      powerLevel = 100;
    }
    if (powerLevel <= 1) {
      powerLevel = 1;
    }
    powerLevel = parseInt(powerLevel + '');
    this.dBArray.push(powerLevel);
  }
  // 停止
  async stop() {
    console.log('停止录音。。。。。。。。。。。。。。');
    recorder.stop();
    this.isRecording = false;
    this.startTime = null;
    const btyArray = [...this.btyArray];
    const int8Array = new Int8Array(btyArray);
    const arraybuffer = new Int8Array(btyArray).buffer;
    this.btyArray = [];
    // 触发onStop事件
    console.log('录音停止---------------');
    let path = 'createPath is false';
    // 如果要生成保存地址，pcm转wav格式才能播放
    if (this.createPath) {
      const wavArraybuffer = pcmToWavArraybuffer(arraybuffer, 16000, 1, 16);
      const base64 = uni.arrayBufferToBase64(wavArraybuffer);
      path = await base64ToPath(`data:audio/wav;base64,${base64}`);
    }
    this.stopCallback && this.stopCallback({ btyArray, int8Array, arraybuffer, path });
    this.dBArray = [];
  }
  // 持续监听
  onFrameRecorded(callback) {
    if (Object.prototype.toString.call(callback) !== '[object Function]') {
      throw Error('参数不是一个function');
    }
    this.frameRecordedCallback = callback;
  }
  // 退出页面后重置销毁所有的设置
  destroyed() {
    // 停止
    this.stop();
    this.isRecording = false;
    this.startCallback = null;
    this.stopCallback = null;
    this.frameRecordedCallback = null;
    this.startTime = null;
    this.btyArray = [];
    this.dBArray = [];
  }
  arrayToArrayBuffer(array) {
    return new Int8Array(array).buffer;
  }
  strArryToIntArry(array) {
    let length = array.length;
    let result = [];
    for (let i = 0; i < length; i++) {
      if (array[i]) {
        result.push(Number(array[i]));
      }
    }
    return result;
  }
}
export default Record;

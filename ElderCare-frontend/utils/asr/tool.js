function getLocalFilePath(path) {
  if (path.indexOf('_www') === 0 || path.indexOf('_doc') === 0 || path.indexOf('_documents') === 0 || path.indexOf('_downloads') === 0) {
    return path;
  }
  if (path.indexOf('file://') === 0) {
    return path;
  }
  if (path.indexOf('/storage/emulated/0/') === 0) {
    return path;
  }
  if (path.indexOf('/') === 0) {
    var localFilePath = plus.io.convertAbsoluteFileSystem(path);
    if (localFilePath !== path) {
      return localFilePath;
    } else {
      path = path.substr(1);
    }
  }
  return '_www/' + path;
}

function dataUrlToBase64(str) {
  var array = str.split(',');
  return array[array.length - 1];
}

function getNewFileId() {
  return +Date.now() +'speech' ;
}

function biggerThan(v1, v2) {
  var v1Array = v1.split('.');
  var v2Array = v2.split('.');
  var update = false;
  for (var index = 0; index < v2Array.length; index++) {
    var diff = v1Array[index] - v2Array[index];
    if (diff !== 0) {
      update = diff > 0;
      break;
    }
  }
  return update;
}
//  临时路径无法读取，需要把临时路径保存为公共路径（能读取的路径）
export const saveFileSync = tempFilePath => {
  return new Promise((resolve, reject) => {
    uni.saveFile({
      tempFilePath,
      success: function (file) {
        resolve(file.savedFilePath);
      },
      fail: function (error) {
        reject(error);
      }
    });
  });
};

export function pathToBase64(path) {
  return new Promise(function (resolve, reject) {
    if (typeof window === 'object' && 'document' in window) {
      if (typeof FileReader === 'function') {
        var xhr = new XMLHttpRequest();
        xhr.open('GET', path, true);
        xhr.responseType = 'blob';
        xhr.onload = function () {
          if (this.status === 200) {
            let fileReader = new FileReader();
            fileReader.onload = function (e) {
              resolve(e.target.result);
            };
            fileReader.onerror = reject;
            fileReader.readAsDataURL(this.response);
          }
        };
        xhr.onerror = reject;
        xhr.send();
        return;
      }
      var canvas = document.createElement('canvas');
      var c2x = canvas.getContext('2d');
      var img = new Image();
      img.onload = function () {
        canvas.width = img.width;
        canvas.height = img.height;
        c2x.drawImage(img, 0, 0);
        resolve(canvas.toDataURL());
        canvas.height = canvas.width = 0;
      };
      img.onerror = reject;
      img.src = path;
      return;
    }
    if (typeof plus === 'object') {
      plus.io.resolveLocalFileSystemURL(
        getLocalFilePath(path),
        function (entry) {
          entry.file(
            function (file) {
              var fileReader = new plus.io.FileReader();
              fileReader.onload = function (data) {
                resolve(data.target.result);
              };
              fileReader.onerror = function (error) {
                reject(error);
              };
              fileReader.readAsDataURL(file);
            },
            function (error) {
              reject(error);
            }
          );
        },
        function (error) {
          reject(error);
        }
      );
      return;
    }
    if (typeof wx === 'object' && wx.canIUse('getFileSystemManager')) {
      wx.getFileSystemManager().readFile({
        filePath: path,
        encoding: 'base64',
        success: function (res) {
          resolve('data:image/png;base64,' + res.data);
        },
        fail: function (error) {
          reject(error);
        }
      });
      return;
    }
    reject(new Error('not support'));
  });
}

export function base64ToPath(base64) {
  return new Promise(function (resolve, reject) {
    if (typeof window === 'object' && 'document' in window) {
      base64 = base64.split(',');
      var type = base64[0].match(/:(.*?);/)[1];
      var str = atob(base64[1]);
      var n = str.length;
      var array = new Uint8Array(n);
      while (n--) {
        array[n] = str.charCodeAt(n);
      }
      return resolve((window.URL || window.webkitURL).createObjectURL(new Blob([array], { type: type })));
    }
    var extName = base64.split(',')[0].match(/data\:\S+\/(\S+);/);
    if (extName) {
      extName = extName[1];
    } else {
      reject(new Error('base64 error'));
    }
    var fileName = getNewFileId() + '.' + extName;
    if (typeof plus === 'object') {
      var basePath = '_doc';
      var dirPath = 'uniapp_temp';
      var filePath = basePath + '/' + dirPath + '/' + fileName;
      if (!biggerThan(plus.os.name === 'Android' ? '1.9.9.80627' : '1.9.9.80472', plus.runtime.innerVersion)) {
        plus.io.resolveLocalFileSystemURL(
          basePath,
          function (entry) {
            entry.getDirectory(
              dirPath,
              {
                create: true,
                exclusive: false
              },
              function (entry) {
                entry.getFile(
                  fileName,
                  {
                    create: true,
                    exclusive: false
                  },
                  function (entry) {
                    entry.createWriter(function (writer) {
                      writer.onwrite = function () {
                        resolve(filePath);
                      };
                      writer.onerror = reject;
                      writer.seek(0);
                      writer.writeAsBinary(dataUrlToBase64(base64));
                    }, reject);
                  },
                  reject
                );
              },
              reject
            );
          },
          reject
        );
        return;
      }
      var bitmap = new plus.nativeObj.Bitmap(fileName);
      bitmap.loadBase64Data(
        base64,
        function () {
          bitmap.save(
            filePath,
            {},
            function () {
              bitmap.clear();
              resolve(filePath);
            },
            function (error) {
              bitmap.clear();
              reject(error);
            }
          );
        },
        function (error) {
          bitmap.clear();
          reject(error);
        }
      );
      return;
    }
    if (typeof wx === 'object' && wx.canIUse('getFileSystemManager')) {
      var filePath = wx.env.USER_DATA_PATH + '/' + fileName;
      wx.getFileSystemManager().writeFile({
        filePath: filePath,
        data: dataUrlToBase64(base64),
        encoding: 'base64',
        success: function () {
          resolve(filePath);
        },
        fail: function (error) {
          reject(error);
        }
      });
      return;
    }
    reject(new Error('not support'));
  });
}

// 合并多个arrayBuffer
/**
 * 
 * @param {*} arrays  arraybuffer组成的数组
 * @returns 
 */
export function mergeArrayBuffers(arrays) {
  let totalLen = 0
  for (let i = 0; i < arrays.length; i++) {
      arrays[i] = new Uint8Array(arrays[i]) //全部转成Uint8Array
      totalLen += arrays[i].length
  }
  let res = new Uint8Array(totalLen)
  let offset = 0
  for(let arr of arrays) {
      res.set(arr, offset)
      offset += arr.length
  }

  return res.buffer
}
export function pcmToWavArraybuffer(
  arraybuffer,
  sampleRate,
  numChannels,
  bitsPerSample
) {
  bitsPerSample = bitsPerSample || 16;
  numChannels = numChannels || 1;
  sampleRate = sampleRate || 16000; // 1. 选择正确的PCM类型

  let pcm;
  if (bitsPerSample === 16) {
    pcm = new Int16Array(arraybuffer);
  } else if (bitsPerSample === 8) {
    pcm = new Uint8Array(arraybuffer);
  } else {
    throw new Error("Only 8 or 16 bitsPerSample supported");
  } // 2. 计算数据长度

  const byteRate = (sampleRate * numChannels * bitsPerSample) / 8;
  const blockAlign = (numChannels * bitsPerSample) / 8;
  const subChunk2Size = (pcm.length * bitsPerSample) / 8;
  const chunkSize = 36 + subChunk2Size; // 3. 构建WAV头部

  function u32ToArray(i) {
    return [i & 0xff, (i >> 8) & 0xff, (i >> 16) & 0xff, (i >> 24) & 0xff];
  }
  function u16ToArray(i) {
    return [i & 0xff, (i >> 8) & 0xff];
  }
  let wavHeader = [
    // RIFF identifier
    0x52,
    0x49,
    0x46,
    0x46,
    ...u32ToArray(chunkSize), // WAVE
    0x57,
    0x41,
    0x56,
    0x45, // fmt chunk
    0x66,
    0x6d,
    0x74,
    0x20,
    ...u32ToArray(16), // Subchunk1Size for PCM
    ...u16ToArray(1), // AudioFormat PCM = 1
    ...u16ToArray(numChannels),
    ...u32ToArray(sampleRate),
    ...u32ToArray(byteRate),
    ...u16ToArray(blockAlign),
    ...u16ToArray(bitsPerSample), // data chunk
    0x64,
    0x61,
    0x74,
    0x61,
    ...u32ToArray(subChunk2Size),
  ];

  let wavHeaderUint8 = new Uint8Array(wavHeader); // 4. PCM数据转Uint8Array

  let pcmUint8;
  if (bitsPerSample === 16) {
    pcmUint8 = new Uint8Array(pcm.buffer);
  } else {
    pcmUint8 = pcm;
  } // 5. 合并头部和数据

  let mergedArray = new Uint8Array(wavHeaderUint8.length + pcmUint8.length);
  mergedArray.set(wavHeaderUint8, 0);
  mergedArray.set(pcmUint8, wavHeaderUint8.length);

  return mergedArray.buffer; // 返回ArrayBuffer
}
// 如果是临时路径
// 使用 saveFileSync，将选择的图片另行保存到本地，并获取保存的地址
// const path = await saveFileSync(tempFilePath);
// const base64 = await pathToBase64(path);


// 如果不是临时路径，能直接访问目录
// const base64 = await pathToBase64(path);


// base64ToPath就没啥好说的，直接用


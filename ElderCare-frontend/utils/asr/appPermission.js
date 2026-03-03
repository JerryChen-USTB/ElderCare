/**
 * 本模块封装了Android、iOS的应用权限判断、打开应用权限设置界面、以及位置系统服务是否开启
 */

var isIos;
// #ifdef APP-PLUS
isIos = plus.os.name == 'iOS';
// #endif

// 判断推送权限是否开启
function judgeIosPermissionPush() {
  var result = false;
  var UIApplication = plus.ios.import('UIApplication');
  var app = UIApplication.sharedApplication();
  var enabledTypes = 0;
  if (app.currentUserNotificationSettings) {
    var settings = app.currentUserNotificationSettings();
    enabledTypes = settings.plusGetAttribute('types');
    console.log('enabledTypes1:' + enabledTypes);
    if (enabledTypes == 0) {
      console.log('推送权限没有开启');
    } else {
      result = true;
      console.log('已经开启推送功能!');
    }
    plus.ios.deleteObject(settings);
  } else {
    enabledTypes = app.enabledRemoteNotificationTypes();
    if (enabledTypes == 0) {
      console.log('推送权限没有开启!');
    } else {
      result = true;
      console.log('已经开启推送功能!');
    }
    console.log('enabledTypes2:' + enabledTypes);
  }
  plus.ios.deleteObject(app);
  plus.ios.deleteObject(UIApplication);
  return result;
}

// 判断定位权限是否开启
function judgeIosPermissionLocation() {
  var result = false;
  var cllocationManger = plus.ios.import('CLLocationManager');
  var status = cllocationManger.authorizationStatus();
  result = status != 2;
  console.log('定位权限开启：' + result);
  plus.ios.deleteObject(cllocationManger);
  return result;
}

// 判断麦克风权限是否开启
function judgeIosPermissionRecord() {
  var result = false;
  var avaudiosession = plus.ios.import('AVAudioSession');
  var avaudio = avaudiosession.sharedInstance();
  var permissionStatus = avaudio.recordPermission();
  console.log('permissionStatus:' + permissionStatus);
  if (permissionStatus == 1684369017 ) {
    console.log('用户拒绝权限');
  } else {
    result = true;
    console.log('麦克风权限已经开启/还未询问授权');
  }
  plus.ios.deleteObject(avaudiosession);
  return result;
}

// 判断相机权限是否开启
function judgeIosPermissionCamera() {
  var result = false;
  var AVCaptureDevice = plus.ios.import('AVCaptureDevice');
  var authStatus = AVCaptureDevice.authorizationStatusForMediaType('vide');
  console.log('authStatus:' + authStatus);
  if (authStatus == 3) {
    result = true;
    console.log('相机权限已经开启');
  } else {
    console.log('相机权限没有开启');
  }
  plus.ios.deleteObject(AVCaptureDevice);
  return result;
}

// 判断相册权限是否开启
function judgeIosPermissionPhotoLibrary() {
  var result = false;
  var PHPhotoLibrary = plus.ios.import('PHPhotoLibrary');
  var authStatus = PHPhotoLibrary.authorizationStatus();
  console.log('authStatus:' + authStatus);
  if (authStatus == 3) {
    result = true;
    console.log('相册权限已经开启');
  } else {
    console.log('相册权限没有开启');
  }
  plus.ios.deleteObject(PHPhotoLibrary);
  return result;
}

// 判断通讯录权限是否开启
function judgeIosPermissionContact() {
  var result = false;
  var CNContactStore = plus.ios.import('CNContactStore');
  var cnAuthStatus = CNContactStore.authorizationStatusForEntityType(0);
  if (cnAuthStatus == 3) {
    result = true;
    console.log('通讯录权限已经开启');
  } else {
    console.log('通讯录权限没有开启');
  }
  plus.ios.deleteObject(CNContactStore);
  return result;
}

// 判断日历权限是否开启
function judgeIosPermissionCalendar() {
  var result = false;
  var EKEventStore = plus.ios.import('EKEventStore');
  var ekAuthStatus = EKEventStore.authorizationStatusForEntityType(0);
  if (ekAuthStatus == 3) {
    result = true;
    console.log('日历权限已经开启');
  } else {
    console.log('日历权限没有开启');
  }
  plus.ios.deleteObject(EKEventStore);
  return result;
}

// 判断备忘录权限是否开启
function judgeIosPermissionMemo() {
  var result = false;
  var EKEventStore = plus.ios.import('EKEventStore');
  var ekAuthStatus = EKEventStore.authorizationStatusForEntityType(1);
  if (ekAuthStatus == 3) {
    result = true;
    console.log('备忘录权限已经开启');
  } else {
    console.log('备忘录权限没有开启');
  }
  plus.ios.deleteObject(EKEventStore);
  return result;
}
// 获取安卓版本
function getAndroidVersion() {
  var Build = plus.android.importClass('android.os.Build');
  return Build.VERSION.SDK_INT;
}

// Android权限查询
function requestAndroidPermission(permissionText) {
  // 区分安卓版本，安卓10以上（29），用ACCESS_BACKGROUND_LOCATION，
  const androidVersion = getAndroidVersion();
  console.log('%c 🍎 androidVersion================: ', 'font-size:20px;background-color: #7F2B82;color:#fff;', androidVersion);
  const locationCode = androidVersion > 28 ? 'android.permission.ACCESS_BACKGROUND_LOCATION':'android.permission.ACCESS_FINE_LOCATION';
  const permissionTextToId = {
    位置权限: locationCode,
    摄像头权限: 'android.permission.CAMERA',
    '外部存储(含相册)读取权限': 'android.permission.READ_EXTERNAL_STORAGE',
    '外部存储(含相册)写入权限': 'android.permission.WRITE_EXTERNAL_STORAGE	',
    麦克风权限: 'android.permission.RECORD_AUDIO',
    通讯录读取权限: 'android.permission.READ_CONTACTS',
    通讯录写入权限: 'android.permission.WRITE_CONTACTS',
    日历读取权限: 'android.permission.READ_CALENDAR',
    日历写入权限: 'android.permission.WRITE_CALENDAR',
    短信读取权限: 'android.permission.READ_SMS',
    短信发送权限: 'android.permission.SEND_SMS',
    拨打电话权限: 'android.permission.CALL_PHONE'
  };
  return new Promise((resolve, reject) => {
    plus.android.requestPermissions(
      [permissionTextToId[permissionText]], // 理论上支持多个权限同时查询，但实际上本函数封装只处理了一个权限的情况。有需要的可自行扩展封装
      function (resultObj) {
        console.log('%c 🍾 权限询问结果resultObj===========: ', 'font-size:20px;background-color: #2EAFB0;color:#fff;', resultObj);
        var result = 0;
        for (var i = 0; i < resultObj.granted.length; i++) {
          var grantedPermission = resultObj.granted[i];
          console.log('已获取的权限：' + grantedPermission);
          result = 1;
        }
        for (var i = 0; i < resultObj.deniedPresent.length; i++) {
          var deniedPresentPermission = resultObj.deniedPresent[i];
          console.log('拒绝本次申请的权限：' + deniedPresentPermission);
          result = 0;
        }
        for (var i = 0; i < resultObj.deniedAlways.length; i++) {
          var deniedAlwaysPermission = resultObj.deniedAlways[i];
          console.log('永久拒绝申请的权限：' + deniedAlwaysPermission);
          result = -1;
        }
        resolve(result);
      },
      function (error) {
        console.log('申请权限错误：' + error.code + ' = ' + error.message);
        resolve({
          code: error.code,
          message: error.message
        });
      }
    );
  });
}

// 使用一个方法，根据参数判断权限
function judgeIosPermission(permissionName) {
  const changeMap = {
    位置权限: 'location',
    摄像头权限: 'camera',
    相册权限: 'photoLibrary	',
    麦克风权限: 'record',
    通讯录权限: 'contact	',
    日历权限: 'calendar',
    备忘录权限: 'memo'
  };
  if (changeMap[permissionName] == 'location') {
    return judgeIosPermissionLocation();
  } else if (changeMap[permissionName] == 'camera') {
    return judgeIosPermissionCamera();
  } else if (changeMap[permissionName] == 'photoLibrary') {
    return judgeIosPermissionPhotoLibrary();
  } else if (changeMap[permissionName] == 'record') {
    return judgeIosPermissionRecord();
  } else if (changeMap[permissionName] == 'push') {
    return judgeIosPermissionPush();
  } else if (changeMap[permissionName] == 'contact') {
    return judgeIosPermissionContact();
  } else if (changeMap[permissionName] == 'calendar') {
    return judgeIosPermissionCalendar();
  } else if (changeMap[permissionName] == 'memo') {
    return judgeIosPermissionMemo();
  }
  return false;
}

// 跳转到**应用**的权限页面
function gotoAppPermissionSetting() {
  if (isIos) {
    var UIApplication = plus.ios.import('UIApplication');
    var application2 = UIApplication.sharedApplication();
    var NSURL2 = plus.ios.import('NSURL');
    var setting2 = NSURL2.URLWithString('app-settings:');
    application2.openURL(setting2);

    plus.ios.deleteObject(setting2);
    plus.ios.deleteObject(NSURL2);
    plus.ios.deleteObject(application2);
  } else {
    var Intent = plus.android.importClass('android.content.Intent');
    var Settings = plus.android.importClass('android.provider.Settings');
    var Uri = plus.android.importClass('android.net.Uri');
    var mainActivity = plus.android.runtimeMainActivity();
    var intent = new Intent();
    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    var uri = Uri.fromParts('package', mainActivity.getPackageName(), null);
    intent.setData(uri);
    mainActivity.startActivity(intent);
  }
}

// 检查系统的设备服务是否开启
function checkSystemEnableLocation() {
  if (isIos) {
    var result = false;
    var cllocationManger = plus.ios.import('CLLocationManager');
    var result = cllocationManger.locationServicesEnabled();
    console.log('系统定位开启:' + result);
    plus.ios.deleteObject(cllocationManger);
    return result;
  } else {
    var context = plus.android.importClass('android.content.Context');
    var locationManager = plus.android.importClass('android.location.LocationManager');
    var main = plus.android.runtimeMainActivity();
    var mainSvr = main.getSystemService(context.LOCATION_SERVICE);
    var result = mainSvr.isProviderEnabled(locationManager.GPS_PROVIDER);
    console.log('系统定位开启:' + result);
    return result;
  }
}
// 询问是否有麦克风权限，如果没有就调起授权，兼容android和ios
async function authForApp(permissionName = '麦克风权限', context) {
  console.log('%c 🥪 isIos: ', 'font-size:20px;background-color: #FCA650;color:#fff;', isIos);
  // 如果是位置权限的话
  if (permissionName === '位置权限') {
    const open = checkSystemEnableLocation();
    if (!open) {
      // 如果没有打开，那么久告知用户打开
      uni.showToast({
        title: '请开启系统定位功能',
        icon: 'none',
        duration: 1000
      });
      return false;
    }
  }
  // 处理是ios的情况
  if (isIos) {
    // 判断iOS上是否给予位置权限，有权限返回true，否则返回false
    const check = judgeIosPermission(permissionName);
    if (check) {
      console.log(permissionName + '已经开启');
      return true;
    } else {
      ((context && context.uiModal) || uni.showModal)({
        title: '权限未分配',
        content: permissionName + '未授权，是否授予？',
        showCancel: false,
        success: function (res) {
          gotoAppPermissionSetting();
        }
      });
      return false;
    }
  } else {
    // 处理是安卓的情况
    var result = await requestAndroidPermission(permissionName);
    console.log('%c 🌮 安卓权限控制：requestAndroidPermission: ', 'font-size:20px;background-color: #4b4b4b;color:#fff;', result);
    var strStatus;
    if (result == 1) {
      strStatus = '已获得授权';
    } else if (result == 0) {
      strStatus = '未获得授权';
    } else {
      strStatus = '被永久拒绝权限';
    }
    const yes = strStatus === '已获得授权';
    console.log(permissionName + strStatus);
    if (!yes) {
      console.log('%c 🍝 context: ', 'font-size:20px;background-color: #42b983;color:#fff;', context);
      let content = permissionName + strStatus + '，需要打开授权管理，开启该授权';
      if (permissionName === '位置权限') {
        const androidVersion = getAndroidVersion();
        content = androidVersion > 28 ? '位置权限不足，请打开授权管理，定位权限选择：始终允许' : '位置权限不足，请授予定位权限';
      }
      ((context && context.uiModal) || uni.showModal)({
        title: '请进行授权',
        content,
        showCancel: false,
        success: function (res) {
          gotoAppPermissionSetting();
        }
      });
    }

    return yes;
  }
}
export default  {
  judgeIosPermission: judgeIosPermission,
  requestAndroidPermission: requestAndroidPermission,
  authForApp,
  checkSystemEnableLocation: checkSystemEnableLocation,
  gotoAppPermissionSetting: gotoAppPermissionSetting
}

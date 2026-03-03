// 紧急求助推送云函数
'use strict';

exports.main = async (event, context) => {
	console.log('📤 收到推送请求，参数:', event)
	
	try {
		// 动态获取 appId，避免硬编码
		// 方式1: 从 context 中获取
		const appId = context.APPID || "__UNI__1BC61C0"; // 提供默认值作为后备
		console.log('📱 当前应用ID:', appId);
		
		// 创建推送管理器
		const uniPush = uniCloud.getPushManager({appId: appId});
		
		// 使用传入的参数发送推送
		const result = await uniPush.sendMessage({
			"push_clientid": event.push_clientid,
			"title": event.title,
			"content": event.content,
			"payload": event.payload
		});
		
		console.log('✅ 推送结果:', result);
		return result;
		
	} catch (error) {
		console.error('❌ 推送失败:', error);
		return {
			errCode: -1,
			errMsg: error.message || '推送失败'
		};
	}
};

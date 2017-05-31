package com.huawei.esdk.uc;

import com.huawei.common.LocalBroadcast.LocalBroadcastProc;
import com.huawei.common.LocalBroadcast.ReceiveData;
import com.huawei.common.constant.CustomBroadcastConst;
import com.huawei.common.constant.UCResource;
import com.huawei.data.base.BaseResponseData;

import android.content.Intent;

/**
 * 这个类是临时添加的，用来处理富媒体发送进度及发送结束消息收不到的问题，最终的解决方案未定，这个只是临时方案
 * 
 * 
 */
public class CommonProc implements LocalBroadcastProc {
	@Override
	public boolean onProc(Intent intent, ReceiveData rd) {
		rd.action = intent.getAction();

		if (CustomBroadcastConst.ACTION_SEND_MESSAGE_RESPONSE.equals(rd.action)
				|| CustomBroadcastConst.ACTION_GROUPSEND_CHAT.equals(rd.action)) {
			rd.result = intent.getIntExtra(UCResource.SERVICE_RESPONSE_RESULT,
					UCResource.REQUEST_OK);

			Object obj = intent
					.getSerializableExtra(UCResource.SERVICE_RESPONSE_DATA);

			if (obj instanceof BaseResponseData) {
				rd.data = (BaseResponseData) obj;
			}

			return true;
		} else {
			return false;
		}

	}

	private boolean onKickedBySVNGateway(Intent it) {
		return false;
	}

}

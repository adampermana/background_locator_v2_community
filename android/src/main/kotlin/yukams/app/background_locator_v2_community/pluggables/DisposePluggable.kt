package yukams.app.background_locator_v2_community.pluggables

import android.content.Context
import android.os.Handler
import io.flutter.plugin.common.MethodChannel
import yukams.app.background_locator_v2_community.IsolateHolderService
import yukams.app.background_locator_v2_community.Keys
import yukams.app.background_locator_v2_community.PreferencesManager

class DisposePluggable : Pluggable {
    override fun setCallback(context: Context, callbackHandle: Long) {
        PreferencesManager.Companion.setCallbackHandle(context, Keys.Companion.DISPOSE_CALLBACK_HANDLE_KEY, callbackHandle)
    }

    override fun onServiceDispose(context: Context) {
        (PreferencesManager.Companion.getCallbackHandle(context, Keys.Companion.DISPOSE_CALLBACK_HANDLE_KEY))?.let { disposeCallback ->
            IsolateHolderService.Companion.getBinaryMessenger(context)?.let { binaryMessenger ->
                val backgroundChannel = MethodChannel(binaryMessenger, Keys.Companion.BACKGROUND_CHANNEL_ID)
                Handler(context.mainLooper)
                    .post {
                        backgroundChannel.invokeMethod(
                            Keys.Companion.BCM_DISPOSE,
                            hashMapOf(Keys.Companion.ARG_DISPOSE_CALLBACK to disposeCallback)
                        )
                    }
            }
        }
    }
}
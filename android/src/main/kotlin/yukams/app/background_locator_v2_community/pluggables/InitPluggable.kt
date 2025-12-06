package yukams.app.background_locator_v2_community.pluggables

import android.content.Context
import android.os.Handler
import io.flutter.plugin.common.MethodChannel
import yukams.app.background_locator_v2_community.IsolateHolderService
import yukams.app.background_locator_v2_community.Keys
import yukams.app.background_locator_v2_community.PreferencesManager

class InitPluggable : Pluggable {
    private var isInitCallbackCalled = false

    override fun setCallback(context: Context, callbackHandle: Long) {
        PreferencesManager.Companion.setCallbackHandle(context, Keys.Companion.INIT_CALLBACK_HANDLE_KEY, callbackHandle)

    }

    override fun onServiceStart(context: Context) {
        if (!isInitCallbackCalled) {
            (PreferencesManager.Companion.getCallbackHandle(context, Keys.Companion.INIT_CALLBACK_HANDLE_KEY))?.let { initCallback ->
                IsolateHolderService.Companion.getBinaryMessenger(context)?.let { binaryMessenger ->
                    val initialDataMap = PreferencesManager.Companion.getDataCallback(context, Keys.Companion.INIT_DATA_CALLBACK_KEY)
                    val backgroundChannel = MethodChannel(binaryMessenger, Keys.Companion.BACKGROUND_CHANNEL_ID)
                    Handler(context.mainLooper)
                        .post {
                            backgroundChannel.invokeMethod(
                                Keys.Companion.BCM_INIT,
                                hashMapOf(
                                    Keys.Companion.ARG_INIT_CALLBACK to initCallback,
                                    Keys.Companion.ARG_INIT_DATA_CALLBACK to initialDataMap
                                )
                            )
                        }
                }
            }
            isInitCallbackCalled = true
        }
    }

    override fun onServiceDispose(context: Context) {
        isInitCallbackCalled = false
    }

    fun setInitData(context: Context, data: Map<*, *>) {
        PreferencesManager.Companion.setDataCallback(context, Keys.Companion.INIT_DATA_CALLBACK_KEY, data)
    }
}
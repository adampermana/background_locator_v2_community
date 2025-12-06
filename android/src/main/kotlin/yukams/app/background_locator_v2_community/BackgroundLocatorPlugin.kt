package yukams.app.background_locator_v2_community

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.PluginRegistry
import yukams.app.background_locator_v2_community.pluggables.DisposePluggable
import yukams.app.background_locator_v2_community.pluggables.InitPluggable
import kotlin.collections.containsKey
import kotlin.collections.get

class BackgroundLocatorPlugin
    : MethodCallHandler, FlutterPlugin, PluginRegistry.NewIntentListener, ActivityAware {
    var context: Context? = null
    private var activity: Activity? = null

    companion object {
        @JvmStatic
        private var channel: MethodChannel? = null

        @JvmStatic
        private fun sendResultWithDelay(context: Context, result: MethodChannel.Result?, value: Boolean, delay: Long) {
            context.mainLooper.let {
                Handler(it).postDelayed({
                    result?.success(value)
                }, delay)
            }
        }

        @SuppressLint("MissingPermission")
        @JvmStatic
        private fun registerLocator(context: Context,
                                    args: Map<Any, Any>,
                                    result: MethodChannel.Result?) {
            if (IsolateHolderService.Companion.isServiceRunning) {
                // The service is running already
                Log.d("BackgroundLocatorPlugin", "Locator service is already running")
                result?.success(true)
                return
            }

            Log.d("BackgroundLocatorPlugin",
                    "start locator with ${PreferencesManager.Companion.getLocationClient(context)} client")

            val callbackHandle = args[Keys.Companion.ARG_CALLBACK] as Long
            PreferencesManager.Companion.setCallbackHandle(context, Keys.Companion.CALLBACK_HANDLE_KEY, callbackHandle)

            val notificationCallback = args[Keys.Companion.ARG_NOTIFICATION_CALLBACK] as? Long
            PreferencesManager.Companion.setCallbackHandle(context, Keys.Companion.NOTIFICATION_CALLBACK_HANDLE_KEY, notificationCallback)

            // Call InitPluggable with initCallbackHandle
            (args[Keys.Companion.ARG_INIT_CALLBACK] as? Long)?.let { initCallbackHandle ->
                val initPluggable = InitPluggable()
                initPluggable.setCallback(context, initCallbackHandle)

                // Set init data if available
                (args[Keys.Companion.ARG_INIT_DATA_CALLBACK] as? Map<*, *>)?.let { initData ->
                    initPluggable.setInitData(context, initData)
                }
            }

            // Call DisposePluggable with disposeCallbackHandle
            (args[Keys.Companion.ARG_DISPOSE_CALLBACK] as? Long)?.let {
                val disposePluggable = DisposePluggable()
                disposePluggable.setCallback(context, it)
            }

            val settings = args[Keys.Companion.ARG_SETTINGS] as Map<*, *>

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_DENIED) {

                val msg = "'registerLocator' requires the ACCESS_FINE_LOCATION permission."
                result?.error(msg, null, null)
                return
            }

            startIsolateService(context, settings)

            // We need to know when the service binded exactly, there is some delay between starting a
            // service and it's binding
            // HELP WANTED: I couldn't find a better way to handle this, so any help or suggestion would be appreciated
            sendResultWithDelay(context, result, true, 1000)
        }

        @JvmStatic
        private fun startIsolateService(context: Context, settings: Map<*, *>) {
            Log.e("BackgroundLocatorPlugin", "startIsolateService")
            val intent = Intent(context, IsolateHolderService::class.java)
            intent.action = IsolateHolderService.Companion.ACTION_START
            intent.putExtra(
                Keys.Companion.SETTINGS_ANDROID_NOTIFICATION_CHANNEL_NAME,
                    settings[Keys.Companion.SETTINGS_ANDROID_NOTIFICATION_CHANNEL_NAME] as? String)
            intent.putExtra(
                Keys.Companion.SETTINGS_ANDROID_NOTIFICATION_TITLE,
                    settings[Keys.Companion.SETTINGS_ANDROID_NOTIFICATION_TITLE] as? String)
            intent.putExtra(
                Keys.Companion.SETTINGS_ANDROID_NOTIFICATION_MSG,
                    settings[Keys.Companion.SETTINGS_ANDROID_NOTIFICATION_MSG] as? String)
            intent.putExtra(
                Keys.Companion.SETTINGS_ANDROID_NOTIFICATION_BIG_MSG,
                    settings[Keys.Companion.SETTINGS_ANDROID_NOTIFICATION_BIG_MSG] as? String)
            intent.putExtra(
                Keys.Companion.SETTINGS_ANDROID_NOTIFICATION_ICON,
                    settings[Keys.Companion.SETTINGS_ANDROID_NOTIFICATION_ICON] as? String)
            intent.putExtra(
                Keys.Companion.SETTINGS_ANDROID_NOTIFICATION_ICON_COLOR,
                    settings[Keys.Companion.SETTINGS_ANDROID_NOTIFICATION_ICON_COLOR] as? Long)
            intent.putExtra(Keys.Companion.SETTINGS_INTERVAL, settings[Keys.Companion.SETTINGS_INTERVAL] as? Int)
            intent.putExtra(Keys.Companion.SETTINGS_ACCURACY, settings[Keys.Companion.SETTINGS_ACCURACY] as? Int)
            intent.putExtra(Keys.Companion.SETTINGS_DISTANCE_FILTER, settings[Keys.Companion.SETTINGS_DISTANCE_FILTER] as? Double)

            if (settings.containsKey(Keys.Companion.SETTINGS_ANDROID_WAKE_LOCK_TIME)) {
                intent.putExtra(
                    Keys.Companion.SETTINGS_ANDROID_WAKE_LOCK_TIME,
                        settings[Keys.Companion.SETTINGS_ANDROID_WAKE_LOCK_TIME] as Int)
            }

            if (PreferencesManager.Companion.getCallbackHandle(context, Keys.Companion.INIT_CALLBACK_HANDLE_KEY) != null) {
                intent.putExtra(Keys.Companion.SETTINGS_INIT_PLUGGABLE, true)
            }
            if (PreferencesManager.Companion.getCallbackHandle(context, Keys.Companion.DISPOSE_CALLBACK_HANDLE_KEY) != null) {
                intent.putExtra(Keys.Companion.SETTINGS_DISPOSABLE_PLUGGABLE, true)
            }

            ContextCompat.startForegroundService(context, intent)
        }

        @JvmStatic
        private fun stopIsolateService(context: Context) {
            val intent = Intent(context, IsolateHolderService::class.java)
            intent.action = IsolateHolderService.Companion.ACTION_SHUTDOWN
            Log.d("BackgroundLocatorPlugin", "stopIsolateService => Shutting down locator plugin")
            ContextCompat.startForegroundService(context, intent)
        }

        @JvmStatic
        private fun initializeService(context: Context, args: Map<Any, Any>) {
            val callbackHandle: Long = args[Keys.Companion.ARG_CALLBACK_DISPATCHER] as Long
            setCallbackDispatcherHandle(context, callbackHandle)
        }

        @JvmStatic
        private fun unRegisterPlugin(context: Context, result: MethodChannel.Result?) {
            if (!IsolateHolderService.Companion.isServiceRunning) {
                // The service is not running
                Log.d("BackgroundLocatorPlugin", "Locator service is not running, nothing to stop")
                result?.success(true)
                return
            }

            stopIsolateService(context)

            // We need to know when the service detached exactly, there is some delay between stopping a
            // service and it's detachment
            // HELP WANTED: I couldn't find a better way to handle this, so any help or suggestion would be appreciated
            sendResultWithDelay(context, result, true, 1000)
        }

        @JvmStatic
        private fun isServiceRunning(result: MethodChannel.Result?) {
            result?.success(IsolateHolderService.Companion.isServiceRunning)
        }

        @JvmStatic
        private fun updateNotificationText(context: Context, args: Map<Any, Any>) {
            val intent = Intent(context, IsolateHolderService::class.java)
            intent.action = IsolateHolderService.Companion.ACTION_UPDATE_NOTIFICATION
            if (args.containsKey(Keys.Companion.SETTINGS_ANDROID_NOTIFICATION_TITLE)) {
                intent.putExtra(
                    Keys.Companion.SETTINGS_ANDROID_NOTIFICATION_TITLE,
                        args[Keys.Companion.SETTINGS_ANDROID_NOTIFICATION_TITLE] as String)
            }
            if (args.containsKey(Keys.Companion.SETTINGS_ANDROID_NOTIFICATION_MSG)) {
                intent.putExtra(
                    Keys.Companion.SETTINGS_ANDROID_NOTIFICATION_MSG,
                        args[Keys.Companion.SETTINGS_ANDROID_NOTIFICATION_MSG] as String)
            }
            if (args.containsKey(Keys.Companion.SETTINGS_ANDROID_NOTIFICATION_BIG_MSG)) {
                intent.putExtra(
                    Keys.Companion.SETTINGS_ANDROID_NOTIFICATION_BIG_MSG,
                        args[Keys.Companion.SETTINGS_ANDROID_NOTIFICATION_BIG_MSG] as String)
            }

            ContextCompat.startForegroundService(context, intent)
        }

        @JvmStatic
        private fun setCallbackDispatcherHandle(context: Context, handle: Long) {
            context.getSharedPreferences(Keys.Companion.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                    .edit()
                    .putLong(Keys.Companion.CALLBACK_DISPATCHER_HANDLE_KEY, handle)
                    .apply()
        }

        @JvmStatic
        fun registerAfterBoot(context: Context) {
            val args = PreferencesManager.Companion.getSettings(context)

            val plugin = BackgroundLocatorPlugin()
            plugin.context = context

            initializeService(context, args)

            val settings = args[Keys.Companion.ARG_SETTINGS] as Map<*, *>
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                startIsolateService(context, settings)
            }
        }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            Keys.Companion.METHOD_PLUGIN_INITIALIZE_SERVICE -> {
                val args: Map<Any, Any>? = call.arguments()

                   // save callback dispatcher to use it when device reboots
                PreferencesManager.Companion.saveCallbackDispatcher(context!! , args!!)




                initializeService(context!!, args)
                result.success(true)
            }
            Keys.Companion.METHOD_PLUGIN_REGISTER_LOCATION_UPDATE -> {
                val args: Map<Any, Any>? = call.arguments()

                // save setting to use it when device reboots

                PreferencesManager.Companion.saveSettings(context!!, args!!)

                registerLocator(context!!,
                        args,
                        result)
            }
            Keys.Companion.METHOD_PLUGIN_UN_REGISTER_LOCATION_UPDATE -> {
                unRegisterPlugin(context!!, result)
            }
            Keys.Companion.METHOD_PLUGIN_IS_REGISTER_LOCATION_UPDATE -> isServiceRunning(result)
            Keys.Companion.METHOD_PLUGIN_IS_SERVICE_RUNNING -> isServiceRunning(result)
            Keys.Companion.METHOD_PLUGIN_UPDATE_NOTIFICATION -> {
                if (!IsolateHolderService.Companion.isServiceRunning) {
                    return
                }

                val args: Map<Any, Any>? = call.arguments()

                    updateNotificationText(context!!, args!!)


                result.success(true)
            }
            else -> result.notImplemented()
        }
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        onAttachedToEngine(binding.applicationContext, binding.binaryMessenger)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    }

    private fun onAttachedToEngine(context: Context, messenger: BinaryMessenger) {
        val plugin = BackgroundLocatorPlugin()
        plugin.context = context

        channel = MethodChannel(messenger, Keys.Companion.CHANNEL_ID)
        channel?.setMethodCallHandler(plugin)
    }

    override fun onNewIntent(intent: Intent): Boolean {
        if (intent.action != Keys.Companion.NOTIFICATION_ACTION) {
            // this is not our notification
            return false
        }

        IsolateHolderService.Companion.getBinaryMessenger(context)?.let { binaryMessenger ->
            val notificationCallback =
                PreferencesManager.Companion.getCallbackHandle(
                    activity!!,
                    Keys.Companion.NOTIFICATION_CALLBACK_HANDLE_KEY
                )
            if (notificationCallback != null && IsolateHolderService.Companion.backgroundEngine != null) {
                val backgroundChannel =
                    MethodChannel(
                        binaryMessenger,
                        Keys.Companion.BACKGROUND_CHANNEL_ID
                    )
                activity?.mainLooper?.let {
                    Handler(it)
                        .post {
                            backgroundChannel.invokeMethod(
                                Keys.Companion.BCM_NOTIFICATION_CLICK,
                                hashMapOf(Keys.Companion.ARG_NOTIFICATION_CALLBACK to notificationCallback)
                            )
                        }
                }
            }
        }

        return true
    }

    override fun onDetachedFromActivity() {
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addOnNewIntentListener(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {
    }


}

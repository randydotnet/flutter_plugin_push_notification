package io.flutter.plugins.push

import android.app.Activity
import android.app.Application
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.plugins.push.extra.PushLifecycleCallbacks
import io.flutter.plugins.push.extra.PushPlugin
import io.flutter.plugins.push.extra.PushPluginListener

/**
 * FlutterPluginPushNotificationPlugin
 */
class PushNotificationPlugin(private val registrar: Registrar, private val channel: MethodChannel) : MethodCallHandler {

    private val activity: Activity = registrar.activity()
    private val application: Application = registrar.context() as Application

    private var methodResult: Result? = null

    private var senderID: String? = null


    init {

        PushLifecycleCallbacks.registerCallbacks(this.application)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {

        this.methodResult = result
        if (call.hasArgument("senderID"))
            this.senderID = call.argument("senderID") as String

        when(call.method) {
            METHOD_REGISTER -> this.register()
            METHOD_UNREGISTER -> this.unregister()
            METHOD_MESSAGE_RECEIVED -> this.onMessageReceived()
            METHOD_TOKEN_REFRESH -> this.onTokenRefresh()
            METHOD_ARE_NOTIFICATION_ENABLED -> this.areNotificationsEnabled()
            else -> result.notImplemented()
        }

    }


    fun register() {
        PushPlugin.register(this.application, this.senderID!!, PushPluginListenerImpl(METHOD_REGISTER))
    }

    fun unregister() {
        PushPlugin.unregister(this.application, this.senderID!!, PushPluginListenerImpl(METHOD_UNREGISTER))
    }

    fun onMessageReceived() {
        PushPlugin.setOnMessageReceivedCallback(PushPluginListenerImpl(METHOD_MESSAGE_RECEIVED))
    }

    fun onTokenRefresh() {
        PushPlugin.setOnTokenRefreshCallback(PushPluginListenerImpl(METHOD_TOKEN_REFRESH))
    }

    fun areNotificationsEnabled() {
        val value = PushPlugin.areNotificationsEnabled()


        var data = mutableMapOf(
                "status" to "success",
                "value" to value
        )

        methodResult!!.success(data)
    }

    private inner class PushPluginListenerImpl internal constructor(private val methodName: String) : PushPluginListener {


        override fun success(message: String?, title: String?, data: Any?) {

            val map = mutableMapOf(
                    "status" to "success",
                    "message" to message,
                    "title" to title,
                    "data" to data
            )

            channel.invokeMethod(methodName, map)

        }

        override fun success(message: String?) {

            val map = mutableMapOf(
                    "status" to "success",
                    "message" to message,
                    "data" to "{}"
            )

            channel.invokeMethod(methodName, map)

        }

        override fun error(data: Any?) {

            val map = mutableMapOf(
                    "status" to "error",
                    "message" to data
            )

            channel.invokeMethod(methodName, map)

        }

    }

    companion object {

        private val CHANNEL_NAME = "plugins.flutter.io/push_notification_plugin"
        private val METHOD_REGISTER = "register"
        private val METHOD_UNREGISTER = "unregister"
        private val METHOD_MESSAGE_RECEIVED = "onMessageReceived"
        private val METHOD_TOKEN_REFRESH = "onTokenRefresh"
        private val METHOD_ARE_NOTIFICATION_ENABLED = "areNotificationsEnabled"

        /**
         * Plugin registration.
         */
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), CHANNEL_NAME)
            channel.setMethodCallHandler(PushNotificationPlugin(registrar, channel))
        }
    }
}
package com.pavelrekun.rekado.services.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Process
import com.pavelrekun.rekado.base.BaseActivity
import com.pavelrekun.rekado.services.dialogs.Dialogs
import com.pavelrekun.rekado.services.eventbus.Events
import com.pavelrekun.rekado.services.lakka.LakkaLoader
import com.pavelrekun.rekado.services.logs.LogHelper
import com.pavelrekun.rekado.services.payloads.PayloadHelper
import com.pavelrekun.rekado.services.payloads.PayloadLoader
import com.pavelrekun.rekado.services.utils.SettingsUtils
import com.pavelrekun.rekado.services.utils.SwitchUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class USBReceiver : BaseActivity() {

    private lateinit var device: UsbDevice

    private var usbHandler: USBHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

            LogHelper.log(1, "USB device connected: ${device.deviceName}")

            if (SettingsUtils.checkAutoInjectorEnabled()) {
                PayloadHelper.putChosen(PayloadHelper.find(SettingsUtils.getAutoInjectorPayload())!!)
                injectPayload()
            } else {
                Dialogs.showInjectorSelectorDialog(this)
            }
        }
    }

    private fun injectPayload() {
        if (SwitchUtils.isRCM(device)) {
            usbHandler = PayloadLoader()
        }

        usbHandler?.handleDevice(device)

        LogHelper.log(1, "Payload loading finished for device: " + device.deviceName)

        finishReceiver()
    }

    private fun injectLakka() {
        try {
            if (SwitchUtils.isRCM(device)) {
                usbHandler = LakkaLoader()
                usbHandler?.handleDevice(device)
            }
        } catch (t: Throwable) {
            LogHelper.log(0, "Failed to inject Lakka!")
        }

        finishReceiver()
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onEvent(event: Events.PayloadSelected) {
        injectPayload()
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onEvent(event: Events.PayloadNotSelected) {
        finishReceiver()
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onEvent(event: Events.InjectorMethodNotSelected) {
        finishReceiver()
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onEvent(event: Events.InjectorMethodPayloadSelected) {
        Dialogs.showPayloadsDialog(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onEvent(event: Events.InjectorMethodLakkaSelected) {
        injectLakka()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    private fun finishReceiver() {
        Process.killProcess(Process.myPid())
    }
}
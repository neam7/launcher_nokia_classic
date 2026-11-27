package com.nokia.launcher.ui.codes

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.nokia.launcher.R

/**
 * Codes that classic Nokias understood. The ones with real data open the system screen that has it,
 * since Android hides the IMEI and addresses from apps; the simulated ones explain themselves here.
 */
enum class SecretCode(val code: String, val title: Int) {
    Imei("*#06#", R.string.code_imei),
    Version("*#0000#", R.string.code_version),
    LifeTimer("*#92702689#", R.string.code_life),
    Bluetooth("*#2820#", R.string.code_bt),
    Wlan("*#62209526#", R.string.code_wlan),
    SimClock("*#746025625#", R.string.code_sim_clock),
    FullRateOn("*3370#", R.string.code_efr_on),
    FullRateOff("#3370#", R.string.code_efr_off),
    HalfRateOn("*4720#", R.string.code_half_on),
    HalfRateOff("#4720#", R.string.code_half_off),
    SecurityCode("12345#", R.string.code_security),
    TestMenu("*#*#4636#*#*", R.string.code_test),
    SoftReset("*#7780#", R.string.code_soft_reset),
    Format("*#7370#", R.string.code_format),
    Scanner("*#723#", R.string.code_scanner),
    ;

    /** Where Android really shows it. Null for the simulated codes and the launcher's own ones. */
    val systemScreen: String?
        get() = when (this) {
            // "About phone" has the IMEI, versions, up time and the Wi-Fi and Bluetooth addresses.
            Imei, Version, LifeTimer, Bluetooth, Wlan -> Settings.ACTION_DEVICE_INFO_SETTINGS
            // Android's test menu only runs when typed in the phone app, so the dialer is opened.
            TestMenu -> Intent.ACTION_DIAL
            else -> null
        }

    fun text(context: Context): String = when (this) {
        SimClock -> context.getString(R.string.code_sim_clock_text)
        FullRateOn, HalfRateOn -> context.getString(R.string.code_emulated_on)
        FullRateOff, HalfRateOff -> context.getString(R.string.code_emulated_off)
        SecurityCode -> context.getString(R.string.code_security_text)
        else -> ""
    }
}

fun findCode(dialed: String): SecretCode? = SecretCode.entries.firstOrNull { it.code == dialed }

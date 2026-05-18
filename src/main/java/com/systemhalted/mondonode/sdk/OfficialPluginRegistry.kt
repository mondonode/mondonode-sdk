package com.systemhalted.mondonode.sdk

/**
 * SDK copy of the official MondoNode plugin registry — plugin ID constants only.
 *
 * The core app (mondonode-app) is the source of truth and carries full metadata
 * including Play Store URLs. This copy is synchronized manually at each official release.
 *
 * Plugin authors: use these constants when declaring PluginDependency entries in your
 * PluginManifest so you don't need to hardcode package name strings.
 */
object OfficialPluginRegistry {
    const val PLUGIN_ID_ICMP = "com.systemhalted.mondonode.plugin.icmp"
    const val PLUGIN_ID_DNS = "com.systemhalted.mondonode.plugin.dns"
}

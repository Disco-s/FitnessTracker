// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.android.library") version "8.1.1" apply false
    id("org.jetbrains.kotlin.plugin.parcelize") version "2.0.21" apply false
}
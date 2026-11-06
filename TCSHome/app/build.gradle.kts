plugins { id("com.android.application") }
android {
    namespace = "com.tcs.home"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.tcs.home"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes { release { isMinifyEnabled = false } }
}

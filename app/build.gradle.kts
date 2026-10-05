import com.github.megatronking.stringfog.plugin.StringFogExtension
import com.github.megatronking.stringfog.plugin.StringFogMode
import com.github.megatronking.stringfog.plugin.kg.RandomKeyGenerator

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

apply(plugin = "stringfog")

configure<StringFogExtension> {
    implementation = "com.github.megatronking.stringfog.xor.StringFogImpl"
    enable = true
    // 全应用字符串加密（含根包 MainActivity 的反篡改代码）
    fogPackages = arrayOf(
        "com.xzd1314.aichat",
        "com.xzd1314.aichat.data",
        "com.xzd1314.aichat.ui",
        "com.xzd1314.aichat.util",
        "com.xzd1314.aichat.net",
        "com.xzd1314.aichat.agent",
        "com.xzd1314.aichat.security"
    )
    kg = RandomKeyGenerator()
    mode = StringFogMode.bytes
}

android {
    namespace = "com.xzd1314.aichat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.xzd1314.aichat"
        minSdk = 23
        targetSdk = 35
        versionCode = 4
        versionName = "1.2.1"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("xzd-release.jks")
            storePassword = "xzd1314"
            keyAlias = "xzd"
            keyPassword = "xzd1314"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "proguard-final.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
        // 禁用 Compose source information——防止编译器在 DEX 中嵌入 "类名 (文件名:行号)" 字符串
        freeCompilerArgs += listOf(
            "-P", "plugin:androidx.compose.compiler.plugins.kotlin:sourceInformation=false"
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // Shizuku SDK（Agent 模式通过 Shizuku 执行 shell）
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    // WorkManager（定时任务）
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    // StringFog 运行时解密（XOR 算法）
    implementation("com.github.megatronking.stringfog:xor:5.0.0")
}

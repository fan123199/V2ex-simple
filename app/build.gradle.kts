import java.util.*

plugins {
    id("com.android.application")
    id("com.google.firebase.crashlytics")
    id("com.google.gms.google-services")
    id("kotlin-android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
}

android {

    compileSdkVersion(30)
    defaultConfig {
        applicationId = "im.fdx.v2ex"
        minSdkVersion(21)
        targetSdkVersion(30)
        versionCode = 43
        versionName = "2.6.9"
    }
    lintOptions {
        isCheckReleaseBuilds = false
        isAbortOnError = false
    }
    signingConfigs {

        create("googlePlay" ){
            val properties = Properties().apply {
                load(File("keystore.properties").reader())
            }
            keyAlias = properties.getProperty("keyAlias") as String
            keyPassword =  properties.getProperty("keyPassword")  as String
            storeFile =  file(properties.getProperty("storeFile") as String)
            storePassword = properties.getProperty("storePassword")  as String
        }
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix =  ".debug"
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("googlePlay" )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
    }
}


android.applicationVariants.all { variant ->
    variant.outputs
        .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
        .forEach { output ->
            if(variant.buildType.name == "debug") {
                output.outputFileName = "v2ex-${variant.buildType.name}-${variant.versionName}.apk"
            }
        }
    true
}

dependencies {
    val room_version = "2.3.0"
    //test related, use test when you really need it, or you forget always.
    testImplementation("junit:junit:4.13.2")
    //test end

    //kotlin start
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.anko:anko-commons:0.10.8")
    implementation("androidx.core:core-ktx:1.3.2")
    //kotlin end

    //google start
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.1.0")
    implementation("androidx.legacy:legacy-support-v13:1.0.0")
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.browser:browser:1.3.0")
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("com.google.android.material:material:1.3.0")

    implementation("android.arch.work:work-runtime-ktx:1.0.1")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")

    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    kapt ("androidx.room:room-compiler:$room_version")
    //google end

    implementation("com.google.code.gson:gson:2.8.6")
    implementation("de.hdodenhof:circleimageview:2.2.0")

    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.9.1"))
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor")

    debugImplementation("com.github.ChuckerTeam.Chucker:library:3.1.2")
    releaseImplementation("com.github.ChuckerTeam.Chucker:library-no-op:3.1.2")
    implementation("com.elvishew:xlog:1.6.1")
    implementation("org.jsoup:jsoup:1.11.3")
    implementation("com.github.bumptech.glide:glide:4.11.0")
    kapt ("com.github.bumptech.glide:compiler:4.11.0")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.11.0")
    implementation("io.reactivex.rxjava2:rxjava:2.2.14")
    implementation("com.google.android:flexbox:2.0.1")
    implementation("me.drakeet.multitype:multitype:3.5.0")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("com.github.esafirm.android-image-picker:imagepicker:2.0.0")
    implementation(platform("com.google.firebase:firebase-bom:28.1.0"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
}
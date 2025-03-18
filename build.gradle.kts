// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
}
allprojects {
    repositories {



        configurations.configureEach {
            resolutionStrategy.dependencySubstitution {
                substitute(module("com.gemalto.jp2:jp2-android"))
                    .using(module("com.github.Tgo1014:JP2ForAndroid:1.0.4"))
            }

            resolutionStrategy.force("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")

        }


//        google()
      //  mavenCentral()
    }
}
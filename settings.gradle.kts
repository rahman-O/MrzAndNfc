pluginManagement {
    repositories {
        maven("https://jitpack.io") // ✨ إضافة JitPack هنا
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }


        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {

        maven("https://jitpack.io") // ✨ إضافة JitPack هنا

        google()
        mavenCentral()
    }


}

rootProject.name = "MrzAndNfc"
include(":app")
//include(":mrz")
//include(":imageCropper")
    //include(":MrzGeter")


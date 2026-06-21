pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "ultra-tv-native"
include(":app")

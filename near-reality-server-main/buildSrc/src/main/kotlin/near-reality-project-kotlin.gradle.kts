plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven(url = "https://repo.runelite.net") // RuneLite
}

kotlin {
    jvmToolchain(21)
}

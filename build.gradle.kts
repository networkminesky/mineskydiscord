plugins {
    id("java-library")
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.helpch.at/releases")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    compileOnly("me.clip:placeholderapi:2.12.3")
    compileOnly("net.luckperms:api:5.5")
    compileOnly("com.gitlab.ruany:LiteBansAPI:0.6.1")
    compileOnly("com.github.networkminesky:mainframe:1.3.1-BETA")
    implementation("net.dv8tion:JDA:6.5.0")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}

plugins {
    java
}

group = "com.poro"
version = "0.1.0"
description = "Empire RPG plugin for Poro Server"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    testCompileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    testRuntimeOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")

    // 나중에 연동할 때 사용
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("net.luckperms:api:5.4")

    // Flag store v0.1 PR0: SQLite JDBC (runtime-loaded via plugin.yml libraries:)
    compileOnly("org.xerial:sqlite-jdbc:3.46.1.0")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.test {
    useJUnitPlatform()
}

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
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    testCompileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    testRuntimeOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")

    // 나중에 연동할 때 사용
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("net.luckperms:api:5.4")

    // MythicMobs 5.x — 로컬 JAR (런타임은 서버 플러그인에서 제공)
    compileOnly(files("../../server/plugins/MythicMobs-5.11.2.jar"))

    // IridiumSkyblock 4.x — 로컬 JAR (런타임은 서버 플러그인에서 제공)
    compileOnly(files("../../server/plugins/IridiumSkyblock-4.1.4.jar"))

    // WorldGuard + WorldEdit 7.x — EngineHub Maven (softdepend, 런타임은 서버 플러그인에서 제공)
    // strictly 버전 충돌을 막기 위해 transitive 의존성 제외 후 필요한 것만 명시
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.15-SNAPSHOT") { isTransitive = false }
    compileOnly("com.sk89q.worldguard:worldguard-core:7.0.15-SNAPSHOT") { isTransitive = false }
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.4.2-SNAPSHOT") { isTransitive = false }
    compileOnly("com.sk89q.worldedit:worldedit-core:7.4.2-SNAPSHOT") { isTransitive = false }

    // Gson — Paper가 런타임에 제공하므로 compileOnly
    compileOnly("com.google.code.gson:gson:2.10.1")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.xerial:sqlite-jdbc:3.47.0.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.test {
    useJUnitPlatform()
}

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
    maven("https://maven.enginehub.org/repo/") // FAWE (WorldEdit) 호환 저장소
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

    // 3월드 전환 스프린트 prep: FAWE API (런타임은 plugin.yml softdepend로 제공)
    // Lv5 6×6 청크 비동기 스키매틱 스탬핑 + Lv1 동기 폴백 용도.
    // FAWE 공식 maven 좌표 기준. 정확한 릴리스 태그는 PR-W3 착수 직전 재검토.
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.11.2")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Flag store v0.1 PR3: SQLite in-memory 통합 테스트용(런타임은 plugin.yml libraries:)
    testImplementation("org.xerial:sqlite-jdbc:3.46.1.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.test {
    useJUnitPlatform()
}

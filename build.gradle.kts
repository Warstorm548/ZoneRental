plugins {
    java
    kotlin("jvm") version "2.2.20"
    id("com.gradleup.shadow") version "9.2.2"
}

group = "com.zonerental"
version = "3.3.0"

repositories {
    mavenCentral()

    // Paper Repository
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    // Vault Repository
    maven {
        name = "jitpack"
        url = uri("https://jitpack.io")
    }

    // WorldGuard/WorldEdit Repository
    maven {
        name = "enginehub"
        url = uri("https://maven.enginehub.org/repo/")
    }

    // LuckPerms Repository
    maven {
        name = "sonatype-snapshots"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

configurations.all {
    resolutionStrategy {
        // Force specific versions to resolve conflicts
        force(
            "com.google.guava:guava:33.3.1-jre",           // Use WorldGuard's version
            "com.google.code.gson:gson:2.11.0",            // Use WorldGuard's version
            "it.unimi.dsi:fastutil:8.5.15",                // Use WorldGuard's version
            "org.apache.logging.log4j:log4j-bom:2.24.1"    // Use WorldGuard's version
        )

        // Prefer modules from Paper when conflicts arise
        preferProjectModules()
    }
}

// Local-only test suite using MockK + MockBukkit (src/mockTest). Not part of check/build,
// so GitHub CI never runs it. Run with: ./gradlew mockTest
sourceSets {
    create("mockTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

// Tests need the compileOnly APIs (Paper, WorldGuard, WorldEdit, Vault) on their classpath
configurations.testImplementation {
    extendsFrom(configurations.compileOnly.get())
}
val mockTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}
configurations["mockTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

dependencies {
    // Paper API
    compileOnly("io.papermc.paper:paper-api:1.21.3-R0.1-SNAPSHOT")

    // Vault API
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")

    // WorldGuard (latest version)
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.14") {
        exclude(group = "com.google.guava")
        exclude(group = "com.google.code.gson")
        exclude(group = "it.unimi.dsi")
        exclude(group = "org.apache.logging.log4j")
    }

    // WorldEdit (use compatible version with WorldGuard)
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.16") {
        exclude(group = "com.google.guava")
        exclude(group = "com.google.code.gson")
        exclude(group = "it.unimi.dsi")
        exclude(group = "org.apache.logging.log4j")
    }
    compileOnly("com.sk89q.worldedit:worldedit-core:7.3.16") {
        exclude(group = "com.google.guava")
        exclude(group = "com.google.code.gson")
        exclude(group = "it.unimi.dsi")
        exclude(group = "org.apache.logging.log4j")
    }

    // LuckPerms API
    compileOnly("net.luckperms:api:5.4")

    // EzChestShop - Optional integration (runtime detection only, no compile dependency)
    // The plugin will detect and integrate with EzChestShop at runtime if installed

    // Kotlin Standard Library
    implementation(kotlin("stdlib"))

    // MCCoroutine for Bukkit (async coroutines)
    implementation("com.github.shynixn.mccoroutine:mccoroutine-bukkit-api:2.21.0")
    implementation("com.github.shynixn.mccoroutine:mccoroutine-bukkit-core:2.21.0")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // Unit tests (src/test) - run in CI
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Mock tests (src/mockTest) - local only
    // MockBukkit 4.26.x is the last line built against Paper 1.21.3 (our compile target)
    mockTestImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.26.0")
    mockTestImplementation("io.mockk:mockk:1.14.11")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

// -PrunKnownIssues also runs the @Disabled known-issue tests (expected to fail until the bug is fixed)
tasks.withType<Test>().configureEach {
    if (project.hasProperty("runKnownIssues")) {
        systemProperty("junit.jupiter.conditions.deactivate", "org.junit.*DisabledCondition")
    }
}

tasks.test {
    useJUnitPlatform()
    workingDir = projectDir
    // Consistency tests read these files directly; re-run when they change
    inputs.dir("src/main")
    inputs.files("CHANGELOG.md", "build.gradle.kts")
}

val mockTest by tasks.registering(Test::class) {
    description = "Runs the local-only MockK/MockBukkit test suite (not run by CI)."
    group = "verification"
    testClassesDirs = sourceSets["mockTest"].output.classesDirs
    classpath = sourceSets["mockTest"].runtimeClasspath
    useJUnitPlatform()
    workingDir = projectDir
    // MockK inline mocking attaches a Java agent at runtime (warns on Java 21+ without this)
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    shouldRunAfter(tasks.test)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(
            "version" to project.version,
            "name" to project.name,
            "group" to project.group
        )
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveFileName.set("${project.name}-${project.version}.jar")

    // Explicitly include runtime dependencies (ensures Kotlin stdlib is bundled)
    configurations = listOf(project.configurations.runtimeClasspath.get())

    // Relocate Kotlin stdlib to avoid conflicts with other plugins
    relocate("kotlin", "com.zonerental.shaded.kotlin")

    // Relocate kotlinx.coroutines to avoid conflicts with other plugins
    relocate("kotlinx.coroutines", "com.zonerental.shaded.kotlinx.coroutines")

    // Relocate MCCoroutine to avoid conflicts with other plugins
    relocate("com.github.shynixn.mccoroutine", "com.zonerental.shaded.mccoroutine")

    // Merge service files for proper ServiceLoader support
    mergeServiceFiles()

    // Minimize JAR by removing unused classes (optional)
    // Uncomment if smaller JAR size is needed:
    // minimize()
}

// Replace the default jar with shadowJar
tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

// Set default tasks
defaultTasks("clean", "build")

// Gradle script to build and run the DacWizard project

import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    application // to build JVM applications
    checkstyle  // to analyze Java sourcecode for style violations
}

val isMacOS = DefaultNativePlatform.getCurrentOperatingSystem().isMacOsX()
val javaVersion = JavaVersion.current()
val enableNativeAccess = javaVersion.isCompatibleWith(JavaVersion.VERSION_17)

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClass = "com.github.stephengold.wizard.DacWizard"
}
if (!isMacOS) {
    tasks.register<JavaExec>("runForceDialog") {
        args("--forceDialog")
        description = "Runs the wizard after displaying the Settings dialog."
        mainClass = application.mainClass
    }
    tasks.register<JavaExec>("runShowDialog") {
        args("--showSettingsDialog")
        description = "Runs the wizard, displaying a dialog if no settings found."
        mainClass = application.mainClass
    }
}
tasks.named<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = application.mainClass
    }
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
}

tasks.withType<JavaCompile>().all { // Java compile-time options:
    options.compilerArgs.add("-Xdiags:verbose")
    if (javaVersion.isCompatibleWith(JavaVersion.VERSION_20)) {
        // Suppress warnings that source value 8 is obsolete.
        options.compilerArgs.add("-Xlint:-options")
    }
    options.compilerArgs.add("-Xlint:unchecked")
    //options.setDeprecation(true) // to provide detailed deprecation warnings
    options.encoding = "UTF-8"
    if (javaVersion.isCompatibleWith(JavaVersion.VERSION_1_10)) {
        options.release = 8
    }
}

tasks.withType<JavaExec>().all { // Java runtime options:
    if (isMacOS) {
        jvmArgs("-XstartOnFirstThread")
    }
    args("--openGL3")
    //args("--verbose") // to enable additional log output
    classpath = sourceSets.main.get().getRuntimeClasspath()
    enableAssertions = true
    if (enableNativeAccess) {
        jvmArgs("--enable-native-access=ALL-UNNAMED") // suppress System::load() warning
    }
    //jvmArgs("-verbose:gc")
    jvmArgs("-Xms1024m", "-Xmx1024m") // to enlarge the Java heap
    //jvmArgs("-XX:+UseG1GC", "-XX:MaxGCPauseMillis=10")
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds") // to disable caching of snapshots
}

dependencies {
    if (!isMacOS) {
        runtimeOnly(libs.jme3.awt.dialogs)
    }
    runtimeOnly(libs.jme3.desktop)
    implementation(libs.heart)
    implementation(libs.minie)
    runtimeOnly(libs.lwjgl)
    implementation(libs.jme3.utilities.nifty)
    runtimeOnly(libs.jme3.plugins)
    runtimeOnly(libs.nifty.style.black)
    implementation(libs.wes)

    // DacWizard doesn't use jme3-jogg
    //  -- it is included solely to avoid warnings from AssetConfig:
    runtimeOnly(libs.jme3.jogg)
}

// Register cleanup tasks:

tasks.named("clean") {
    dependsOn("cleanDLLs", "cleanDyLibs", "cleanLogs", "cleanPDBs", "cleanSandbox", "cleanSOs")
}

tasks.register<Delete>("cleanDLLs") { // extracted Windows native libraries
    delete(fileTree(".").matching{ include("*.dll") })
}
tasks.register<Delete>("cleanDyLibs") { // extracted macOS native libraries
    delete(fileTree(".").matching{ include("*.dylib") })
}
tasks.register<Delete>("cleanLogs") { // JVM crash logs
    delete(fileTree(".").matching{ include("hs_err_pid*.log") })
}
tasks.register<Delete>("cleanPDBs") { // Windows program database files
    delete(fileTree(".").matching{ include("*.pdb") })
}
tasks.register<Delete>("cleanSandbox") { // Acorus sandbox
    delete("Written Assets")
}
tasks.register<Delete>("cleanSOs") { // extracted Linux and Android native libraries
    delete(fileTree(".").matching{ include("*.so") })
}

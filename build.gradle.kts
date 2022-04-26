val ktorVersion = "2.0.0"
val kotlinVersion = "1.6.20"
val logbackVersion = "1.2.3"

plugins {

    application
    kotlin("jvm") version "1.6.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.6.20"

    `java-library`
}


java {
    withSourcesJar()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

tasks.jar {
    manifest {
        attributes(mapOf("Implementation-Title" to project.name,
            "Implementation-Version" to project.version))
    }
    duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.INCLUDE

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

group = "me.lizhen"
version = "0.0.1"
application {
    mainClass.set("me.lizhen.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")

}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

dependencies {
    implementation("org.litote.kmongo:kmongo-coroutine:4.5.0")
//    implementation("io.dgraph:dgraph4j:21.12.0")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auto-head-response-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cors-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}
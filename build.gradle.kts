plugins {
    kotlin("jvm") version "1.8.0"
    application
}

group = "me.vlads"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val exposedVersion = "0.40.1"

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.slf4j:slf4j-nop:1.7.30")

    implementation("org.jetbrains.exposed", "exposed-core", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-dao", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)

    implementation("org.postgresql:postgresql:42.5.4")
    implementation("com.zaxxer:HikariCP:3.1.0")

}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("MainKt")
}
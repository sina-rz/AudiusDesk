import org.jetbrains.compose.desktop.application.dsl.TargetFormat


plugins {
    kotlin("jvm") version "2.4.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21"
    id("org.jetbrains.compose") version "1.12.1"
    kotlin("plugin.serialization") version "2.4.21"
}

group = "com.audiusdesk"
version = "1.0.0"


dependencies {

    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.material3:material3:1.9.0")
    implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")


    val ktorVersion = "3.6.0"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-java:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")


    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")


    val coilVersion = "3.6.3"
    implementation("io.coil-kt.coil3:coil-compose:$coilVersion")
    implementation("io.coil-kt.coil3:coil-network-ktor3:$coilVersion")

    implementation("io.github.sina-rz:judio:0.1.7")

    implementation("org.slf4j:slf4j-simple:2.0.17")

    testImplementation(kotlin("test"))
}

tasks.withType<Test> { useJUnitPlatform() }

kotlin {
    jvmToolchain(25)
}

compose.desktop {
    application {
        mainClass = "com.audiusdesk.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "AudiusDesk"
            packageVersion = "1.0.0"
            description = "A modern desktop streaming client for the Audius music network"
            vendor = "AudiusDesk"

            modules("java.net.http", "jdk.crypto.ec")
        }
    }
}

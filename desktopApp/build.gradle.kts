import java.net.URL
import java.net.HttpURLConnection

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    jvmToolchain(17)
}

val vazirmatnResources = layout.buildDirectory.dir("generated/vazirmatn-resources")
val downloadVazirmatnFonts by tasks.registering {
    outputs.dir(vazirmatnResources)
    doLast {
        val output = vazirmatnResources.get().asFile
        val fontFiles = listOf("Vazirmatn-Regular.ttf", "Vazirmatn-Bold.ttf")
        fontFiles.forEach { name ->
            val target = output.resolve("fonts/$name")
            if (!target.exists() || target.length() < 50_000L) {
                target.parentFile.mkdirs()
                val connection = URL(
                    "https://raw.githubusercontent.com/rastikerdar/vazirmatn/master/fonts/ttf/$name"
                ).openConnection() as HttpURLConnection
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.setRequestProperty("User-Agent", "MedFlow-Desktop-Build")
                val bytes = connection.inputStream.use { stream -> stream.readBytes() }
                check(bytes.size > 50_000) { "Downloaded Vazirmatn font is incomplete: $name" }
                target.writeBytes(bytes)
            }
        }
        val license = output.resolve("fonts/OFL.txt")
        if (!license.exists()) {
            license.parentFile.mkdirs()
            val connection = URL("https://raw.githubusercontent.com/rastikerdar/vazirmatn/master/OFL.txt").openConnection()
            val licenseText = connection.getInputStream().use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            }
            license.writeText(licenseText, Charsets.UTF_8)
        }
    }
}

sourceSets.main {
    resources.srcDir(vazirmatnResources)
}

tasks.named("processResources") {
    dependsOn(downloadVazirmatnFonts)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4")
    implementation("net.java.dev.jna:jna:5.15.0")
    testImplementation("junit:junit:4.13.2")
}

compose.desktop {
    application {
        mainClass = "com.flow.desktop.MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi)
            packageName = "MedFlow"
            packageVersion = "1.0.0"
            description = "MedFlow productivity and focus desktop application"
            vendor = "MayT3n"
        }
    }
}

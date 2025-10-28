import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import org.gradle.internal.impldep.com.jcraft.jsch.ChannelSftp
import org.gradle.internal.impldep.com.jcraft.jsch.JSch

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.0.0"
    id("de.eldoria.plugin-yml.bukkit") version "0.6.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("de.nilsdruyen.gradle-ftp-upload-plugin") version "0.4.2"
}

group = "de.unknowncity"
version = "0.1.0"

// REPLACE PaperTemplatePlugin with the plugin name!
val mainClass = "${group}.${rootProject.name.lowercase()}.${project.name}Plugin"
val shadeBasePath = "${group}.${rootProject.name.lowercase()}.libs."

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.unknowncity.de/snapshots")
    maven("https://repo.unknowncity.de/releases")
    maven("https://jitpack.io")
    maven("https://repo.xenondevs.xyz/releases")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.nightexpressdev.com/releases")
    maven {
        url = uri("https://repo.unknowncity.de/private")
        credentials (PasswordCredentials::class) {
            username = System.getenv("MVN_REPO_USERNAME")
            password = System.getenv("MVN_REPO_PASSWORD")
        }
    }
}

dependencies {

    // User interface library
    implementation("xyz.xenondevs.invui", "invui", "2.0.0-alpha.19")

    // Economy system
    compileOnly("su.nightexpress.coinsengine", "CoinsEngine", "2.5.0")

    compileOnly("de.unknowncity.astralib", "astralib-paper-api", "0.6.0-SNAPSHOT")

    compileOnly("io.papermc.paper", "paper-api", "1.21.8-R0.1-SNAPSHOT")
}

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("com.github.mwiede:jsch:2.27.3")
    }
}

bukkit {
    name = "UC-${project.name}"
    version = "${rootProject.version}"

    // REPLACE with fitting description
    description = "Super cool sample plugin"

    author = "UnknownCity"

    main = mainClass

    foliaSupported = false

    apiVersion = "1.21"

    load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD

    // Add dependency plugins
    softDepend = listOf()
    depend = listOf("AstraLib")

    defaultPermission = BukkitPluginDescription.Permission.Default.OP
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    getByName<Test>("test") {
        useJUnitPlatform()
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    shadowJar {
        fun relocateDependency(from : String) = relocate(from, "$shadeBasePath$from")

        relocateDependency("xyz.xenondevs.invui")
    }

    runServer {
        minecraftVersion("1.21.8")

        jvmArgs("-Dcom.mojang.eula.agree=true")

        downloadPlugins {
            // ADD plugins needed for testing
            // E.g: url("https://github.com/EssentialsX/Essentials/releases/download/2.20.1/EssentialsX-2.20.1.jar")
        }
    }

    register("uploadJarToFTP") {
        dependsOn(shadowJar)
        doLast {
            val jarFile = getByName("shadowJar").outputs.files.singleFile

            val host = System.getenv("FTP_SERVER")!!
            val port = System.getenv("FTP_PORT")?.toInt() ?: 22
            val user = System.getenv("FTP_USER")!!
            val password = System.getenv("FTP_PASSWORD")!!

            val jsch = JSch()
            val session = jsch.getSession(user, host, port).apply {
                setPassword(password)
                setConfig("StrictHostKeyChecking", "no")
                connect()
            }

            val channel = session.openChannel("sftp") as ChannelSftp
            channel.connect()
            channel.cd("/plugins")
            channel.put(jarFile.absolutePath, jarFile.name)
            println("Upload successful!")

            channel.disconnect()
            session.disconnect()
        }
    }
}
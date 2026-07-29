plugins {
    java
    idea
    alias(libs.plugins.paper)
    alias(libs.plugins.runpaper)
}

group = project.properties["plugin.group"].toString()
val releaseVersion = providers.gradleProperty("releaseVersion")
    .orElse(project.properties["plugin.version"].toString())
version = releaseVersion.get()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

paperweight.reobfArtifactConfiguration =
    io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven("https://repo.bluecolored.de/releases")
}

dependencies {
    val paperVersion = project.properties["paper.version"].toString()
    paperweight.foliaDevBundle(paperVersion)
    compileOnly("de.bluecolored:bluemap-api:2.7.8")
    testImplementation("de.bluecolored:bluemap-api:2.7.8")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(25)
    }

    jar {
        archiveFileName.set("${project.name}-${project.version}.jar")
    }

    test {
        useJUnitPlatform()
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
        val props = mapOf(
            "name" to project.properties["plugin.name"],
            "version" to project.version,
            "main" to project.properties["plugin.main"],
            "apiVersion" to project.properties["paper.api"],
        )
        inputs.properties(props)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}

runPaper {
    folia {
        registerTask {
            val paperVersion = project.properties["paper.version"].toString()
            serverJar(file("run/folia-paperclip-${paperVersion}-mojmap.jar"))
        }
    }
}

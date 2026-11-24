plugins {
    java
}

group = "io.github.peanutgt"

java {
    // Paper 26.x runs on Java 25.
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.org/repository/maven-public/")
}

// Offline build: pass -PlocalLibs=<folder with paper-api and SkinsRestorer jars>
// to compile against local copies instead of the Maven repositories.
val localLibs = providers.gradleProperty("localLibs").orNull

dependencies {
    if (localLibs != null) {
        compileOnly(fileTree(localLibs) { include("*.jar") })
    } else {
        compileOnly("io.papermc.paper:paper-api:${property("paperApiVersion")}")
        compileOnly("net.skinsrestorer:skinsrestorer-api:${property("skinsRestorerApiVersion")}")
    }
    testImplementation("com.google.code.gson:gson:2.14.0")
    testImplementation("org.jspecify:jspecify:1.0.0")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("plugin.yml") { expand(props) }
}

tasks.jar {
    archiveBaseName.set("HeadsCrafting")
}

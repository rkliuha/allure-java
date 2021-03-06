import io.qameta.allure.gradle.AllureExtension
import io.qameta.allure.gradle.task.AllureReport
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.jvm.tasks.Jar
import ru.vyarus.gradle.plugin.quality.QualityExtension

buildscript {
    repositories {
        maven("https://plugins.gradle.org/m2/")
        mavenLocal()
        jcenter()
    }

    dependencies {
        classpath("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.4")
        classpath("gradle.plugin.com.github.spotbugs:spotbugs-gradle-plugin:1.6.5")
        classpath("io.spring.gradle:dependency-management-plugin:1.0.6.RELEASE")
        classpath("ru.vyarus:gradle-quality-plugin:3.2.0")
    }
}

val linkHomepage by extra("https://qameta.io/allure")
val linkCi by extra("https://ci.qameta.in/job/allure-java_deploy/")
val linkScmUrl by extra("https://github.com/allure-framework/allure-java")
val linkScmConnection by extra("scm:git:git://github.com/allure-framework/allure-java.git")
val linkScmDevConnection by extra("scm:git:ssh://git@github.com:allure-framework/allure-java.git")

val gradleScriptDir by extra("${rootProject.projectDir}/gradle")

tasks.existing(Wrapper::class) {
    gradleVersion = "4.10.2"
    distributionType = Wrapper.DistributionType.ALL
}

plugins {
    java
    id("net.researchgate.release") version "2.7.0"
    id("io.qameta.allure") version "2.5"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

release {
    tagTemplate = "\${version}"
}

val afterReleaseBuild by tasks.existing

configure(listOf(rootProject)) {
    description = "Allure Java"

}

configure(subprojects) {
    val project = this
    group = "io.qameta.allure"
    version = version

    apply(plugin = "java")
    apply(plugin = "maven")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "ru.vyarus.quality")
    apply(plugin = "io.qameta.allure")
    apply(from = "$gradleScriptDir/bintray.gradle")
    apply(from = "$gradleScriptDir/maven-publish.gradle")

    configure<DependencyManagementExtension> {
        imports {
            mavenBom("com.fasterxml.jackson:jackson-bom:2.9.7")
            mavenBom("org.junit:junit-bom:5.3.1")
        }
        dependencies {
            dependency("com.codeborne:selenide:4.12.2")
            dependency("com.github.tomakehurst:wiremock:2.18.0")
            dependency("com.google.inject:guice:4.2.0")
            dependency("com.google.testing.compile:compile-testing:0.15")
            dependency("com.squareup.okhttp3:okhttp:3.10.0")
            dependency("com.squareup.retrofit2:retrofit:2.4.0")
            dependency("commons-io:commons-io:2.6")
            dependency("io.github.benas:random-beans:3.7.0")
            dependency("io.github.glytching:junit-extensions:2.3.0")
            dependency("org.apache.commons:commons-lang3:3.7")
            dependency("org.apache.httpcomponents:httpclient:4.5.6")
            dependency("org.apache.tika:tika-core:1.19.1")
            dependency("org.aspectj:aspectjrt:1.9.1")
            dependency("org.aspectj:aspectjweaver:1.9.1")
            dependency("org.assertj:assertj-core:3.10.0")
            dependency("org.codehaus.groovy:groovy-all:2.5.1")
            dependency("org.freemarker:freemarker:2.3.28")
            dependency("org.jboss.resteasy:resteasy-client:3.6.1.Final")
            dependency("org.jooq:joor-java-8:0.9.9")
            dependency("org.junit-pioneer:junit-pioneer:0.2.2")
            dependency("org.mock-server:mockserver-netty:5.4.1")
            dependency("org.mockito:mockito-core:2.19.0")
            dependency("org.slf4j:slf4j-api:1.7.25")
            dependency("org.slf4j:slf4j-simple:1.7.25")
            dependency("org.springframework.boot:spring-boot-autoconfigure:1.5.14.RELEASE")
            dependency("org.springframework:spring-test:4.3.18.RELEASE")
            dependency("org.springframework:spring-webmvc:4.3.18.RELEASE")
        }
    }

    tasks.named<Jar>("jar") {
        manifest {
            attributes(mapOf(
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version
            ))
        }
    }

    tasks.named<Test>("test") {
        systemProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
        systemProperty("allure.model.indentOutput", "true")
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    tasks.named<ProcessResources>("processTestResources") {
        filesMatching("**/allure.properties") {
            filter {
                it.replace("#project.description#", project.description ?: project.name)
            }
        }
    }

    configure<QualityExtension> {
        configDir = "$gradleScriptDir/quality-configs"
        pmdVersion = "6.9.0"
    }

    configure<AllureExtension> {
        autoconfigure = false
        aspectjweaver = false
    }

    val sourceSets = project.the<SourceSetContainer>()

    val sourceJar by tasks.creating(Jar::class) {
        from(sourceSets.getByName("main").allJava)
        classifier = "sources"
    }

    val javadocJar by tasks.creating(Jar::class) {
        from(tasks.getByName("javadoc"))
        classifier = "javadoc"
    }

    tasks.withType(Javadoc::class) {
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }

    artifacts.add("archives", sourceJar)
    artifacts.add("archives", javadocJar)

    val bintrayUpload by tasks.existing
    afterReleaseBuild {
        dependsOn(bintrayUpload)
    }

    repositories {
        jcenter()
        mavenLocal()
    }
}

allure {
    version = "2.8.1"
    autoconfigure = false
    aspectjweaver = false
    downloadLink = "https://repo.maven.apache.org/maven2/io/qameta/allure/allure-commandline/2.8.1/allure-commandline-2.8.1.zip"
}

val aggregatedReport by tasks.creating(AllureReport::class) {
    clean = true
    resultsDirs = subprojects.map { file("${it.buildDir}/allure-results") }.filter { it.exists() }
}

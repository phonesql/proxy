group = "com.phonesql"
version = "1.0.0"

plugins {
    id("java")
    id("com.diffplug.spotless") version "6.25.0"
    id("com.google.protobuf") version "0.9.5"
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")
    implementation("io.netty:netty-all:4.2.1.Final")
    implementation("com.nimbusds:nimbus-jose-jwt:10.3")
    implementation("org.apache.commons:commons-lang3:3.18.0")
    implementation("com.samskivert:jmustache:1.16")
    implementation("org.postgresql:postgresql:42.7.7")
    implementation("com.google.protobuf:protobuf-java:4.31.1")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.phonesql.proxy.Main"
    }
    from(configurations.runtimeClasspath.get().map(::zipTree))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val googleJavaFormatVersion = "1.19.2"
val htmlTabWidth = 4

spotless {
    format("html") {
        prettier().config(mapOf("tabWidth" to htmlTabWidth, "parser" to "html"))

        target( "src/**/templates/**/*.mustache")
    }
    javascript {
        target( "src/**/static/js/*.js")

        prettier()
    }
    java {
        targetExclude("**/proto/**/*.java")

        googleJavaFormat(googleJavaFormatVersion).aosp().reflowLongStrings().skipJavadocFormatting()
        formatAnnotations()
    }
}
plugins {
    id("org.asciidoctor.jvm.convert") version "4.0.2"
}

val snippetsDir = file("build/generated-snippets")

tasks.jar {
    enabled = false
}

tasks.bootJar {
    enabled = true
    dependsOn(tasks.asciidoctor)
    from("${tasks.asciidoctor.get().outputDir}") {
        into("static/docs")
    }
}

tasks.test {
    outputs.dir(snippetsDir)
}

tasks.asciidoctor {
    inputs.dir(snippetsDir)
    dependsOn(tasks.test)
    baseDirFollowsSourceDir()

    attributes(
        mapOf(
            "snippets" to snippetsDir,
            "source-highlighter" to "highlight.js",
            "toclevels" to "3",
            "sectlinks" to "true"
        )
    )
}

dependencies {
    implementation(projects.modules.domain)
    implementation(projects.modules.application)
    implementation(projects.modules.infrastructure.persistence)
    implementation(projects.modules.external.pgClient)
    implementation(libs.spring.boot.starter.jpa)
    implementation(libs.bundles.bootstrap)
    testImplementation(libs.bundles.test)
    testImplementation(libs.spring.boot.starter.test) {
        exclude(module = "mockito-core")
    }
    testImplementation(libs.spring.mockk)
    testImplementation(libs.spring.restdocs.mockmvc)
    testImplementation(libs.database.h2)
}

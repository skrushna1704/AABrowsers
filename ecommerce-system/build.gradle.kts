plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

group = "com.kododake.ecommerce"
version = "1.0.0"

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.kododake.ecommerce.MainKt")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
    workingDir = projectDir
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

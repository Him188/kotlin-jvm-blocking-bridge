plugins {
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.serialization")
    id("java")
    `maven-publish`
    signing
    id("com.jfrog.bintray")
    // id("com.bmuschko.nexus")
    //  id("io.codearte.nexus-staging")
}

dependencies {
    implementation(kotlin("stdlib"))

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.2.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.2.0")
}

setupPublishing(
    groupId = "net.mamoe",
    artifactId = "kotlin-jvm-blocking-bridge"
)

/*
nexus {
    sign = true
    repositoryUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
    snapshotRepositoryUrl = "https://oss.sonatype.org/content/repositories/snapshots/"
}

nexusStaging {
    packageGroup = "net.mamoe" //optional if packageGroup == project.getGroup()
    // stagingProfileId = "yourStagingProfileId" //when not defined will be got from server using "packageGroup"
}

extraArchive {
    sources = false
    tests = false
    javadoc = false
}*/

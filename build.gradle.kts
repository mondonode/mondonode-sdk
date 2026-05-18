import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.nmcp)
    id("maven-publish")
    id("signing")
}

// ---------------------------------------------------------------------------
// Coordinates
// ---------------------------------------------------------------------------
val sdkGroupId    = "com.systemhalted"
val sdkArtifactId = "mondonode-sdk"
val sdkVersion    = "1.0.0"

// ---------------------------------------------------------------------------
// Android library
// ---------------------------------------------------------------------------
android {
    namespace  = "com.systemhalted.mondonode.sdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        aidl = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coroutines.android)
    implementation(libs.security.crypto)
    testImplementation(libs.junit)
}

// ---------------------------------------------------------------------------
// Dokka — KDoc → HTML Javadoc JAR (required by Maven Central)
// ---------------------------------------------------------------------------
tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets {
        named("main") {
            moduleName.set("MondoNode SDK")
            includes.from("MODULE.md")
            reportUndocumented.set(false)
            skipEmptyPackages.set(true)
            sourceLink {
                localDirectory.set(file("src/main/java"))
                remoteUrl.set(uri("https://github.com/syshlted/mondonode-sdk/blob/main/src/main/java").toURL())
                remoteLineSuffix.set("#L")
            }
        }
    }
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.named("dokkaHtml"))
}

// ---------------------------------------------------------------------------
// Publishing — POM metadata + signing
// ---------------------------------------------------------------------------
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifact(javadocJar)

                groupId    = sdkGroupId
                artifactId = sdkArtifactId
                version    = sdkVersion

                pom {
                    name.set("MondoNode SDK")
                    description.set(
                        "Shared AIDL interfaces, base service classes, authentication helpers, " +
                        "and serializable data models for MondoNode plugin development. " +
                        "Covers monitoring plugins, transformer plugins, router plugins, " +
                        "output plugins, storage plugins, and management apps."
                    )
                    url.set("https://github.com/syshlted/mondonode-sdk")
                    inceptionYear.set("2025")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://github.com/syshlted/mondonode-sdk/blob/main/LICENSE")
                            distribution.set("repo")
                        }
                    }

                    developers {
                        developer {
                            id.set("systemhalted")
                            name.set("SystemHalted")
                            email.set("dev@systemhalted.com")
                            organization.set("SystemHalted")
                            organizationUrl.set("https://systemhalted.com")
                        }
                    }

                    scm {
                        url.set("https://github.com/syshlted/mondonode-sdk")
                        connection.set("scm:git:https://github.com/syshlted/mondonode-sdk.git")
                        developerConnection.set("scm:git:git@github.com:syshlted/mondonode-sdk.git")
                    }

                    issueManagement {
                        system.set("GitHub Issues")
                        url.set("https://github.com/syshlted/mondonode-sdk/issues")
                    }
                }
            }
        }
    }

    // Signing — reads from gradle.properties or environment variables.
    // Required for Maven Central; set signing.keyId / signing.key / signing.password
    // in ~/.gradle/gradle.properties (never commit), or export as GPG_KEY_ID /
    // GPG_SIGNING_KEY / GPG_SIGNING_PASSWORD in CI.
    signing {
        val keyId   = findProperty("signing.keyId")    as String? ?: System.getenv("GPG_KEY_ID")
        val key     = findProperty("signing.key")      as String? ?: System.getenv("GPG_SIGNING_KEY")
        val keyPass = findProperty("signing.password") as String? ?: System.getenv("GPG_SIGNING_PASSWORD")
        useInMemoryPgpKeys(keyId, key, keyPass)
        sign(publishing.publications["release"])
    }
}

// ---------------------------------------------------------------------------
// Maven Central (new Central Portal) — upload via nmcp
// Credentials: generate a User Token at central.sonatype.com → Account → User Token.
// Store in ~/.gradle/gradle.properties as centralPortalUsername / centralPortalPassword,
// or export as CENTRAL_PORTAL_USERNAME / CENTRAL_PORTAL_PASSWORD in CI.
// Publish command: ./gradlew :mondonode-sdk:publishAllPublicationsToCentralPortal
// ---------------------------------------------------------------------------
nmcp {
    // publishAllPublications configures the generated publishAllPublicationsToCentralPortal task.
    publishAllPublications {
        username = findProperty("centralPortalUsername") as String?
            ?: System.getenv("CENTRAL_PORTAL_USERNAME")
        password = findProperty("centralPortalPassword") as String?
            ?: System.getenv("CENTRAL_PORTAL_PASSWORD")
        // "USER_MANAGED": review the deployment at central.sonatype.com before releasing.
        // Change to "AUTOMATIC" once you have a stable release workflow.
        publicationType = "USER_MANAGED"
    }
}

// global build settings for the DacWizard project

rootProject.name = "DacWizard"

dependencyResolutionManagement {
    repositories {
        mavenCentral() // to find libraries released to the Maven Central repository
        maven {
            name = "Central Portal Snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
        //mavenLocal() // to find libraries installed locally
    }
}

// no subprojects

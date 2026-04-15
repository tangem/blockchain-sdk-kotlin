import java.io.FileInputStream
import java.util.Properties

apply(plugin = "maven-publish")

val publishPropsFile = rootProject.file("publish.properties")
val publishProps = Properties().apply {
    if (publishPropsFile.exists()) {
        load(FileInputStream(publishPropsFile))
    }
}

val artifactGroupId: String = findProperty("artifactGroupId") as String?
    ?: publishProps.getProperty("artifactGroupId")
val artifactIdProp: String = findProperty("artifactId") as String?
    ?: publishProps.getProperty("artifactId")
val artifactVersion: String = findProperty("artifactVersion") as String?
    ?: publishProps.getProperty("artifactVersion")

afterEvaluate {
    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("bar") {
                groupId = artifactGroupId
                artifactId = artifactIdProp
                version = artifactVersion

                from(components["release"])

                pom.withXml {
                    val pomNode = asNode()
                    val dependenciesNode = pomNode.get("dependencies") as groovy.util.NodeList
                    if (dependenciesNode.isNotEmpty()) {
                        @Suppress("UNCHECKED_CAST")
                        val deps = (dependenciesNode[0] as groovy.util.Node).children() as List<groovy.util.Node>
                        // Remove self-referencing dependencies
                        deps.filter { dep ->
                            val groupId = (dep.get("groupId") as groovy.util.NodeList)
                            groupId.isNotEmpty() && (groupId[0] as groovy.util.Node).text() == artifactGroupId
                        }.forEach { it.parent().remove(it) }
                        // Change runtime scope to compile
                        deps.filter { dep ->
                            val scope = (dep.get("scope") as groovy.util.NodeList)
                            scope.isNotEmpty() && (scope[0] as groovy.util.Node).text() == "runtime"
                        }.forEach { dep ->
                            val scopeNode = (dep.get("scope") as groovy.util.NodeList)[0] as groovy.util.Node
                            scopeNode.setValue("compile")
                        }
                    }
                }
            }
        }

        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/tangem/blockchain-sdk-kotlin")
                credentials {
                    username = findProperty("githubUser") as String?
                    password = findProperty("githubPass") as String?
                }
            }
        }
    }
}
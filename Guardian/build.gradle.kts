version = "1.0.4"
group = "city.newnan.guardian"
description = "Guardian for NewNanCity."
project.ext["depend"] = listOf("GroupManager")

dependencies {
    // API
    // https://github.com/ElgarL/GroupManager
    listOf("com.github.ElgarL:groupmanager:2.9").forEach { compileOnly(it); testImplementation(it) }

    // Utils
    implementation("me.lucko:helper:5.6.14")
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation("com.github.NewNanCity:Violet:2.1.5")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")

    // mysql
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("org.ktorm:ktorm-core:3.6.0")
}
version = "1.0.4"
group = "city.newnan.guardian"
description = "Guardian for NewNanCity."
project.ext["softDepend"] = listOf("GroupManager")
project.ext["loadBefore"] = listOf("GroupManager")

tasks.shadowJar {
    // Violet
    relocate("city.newnan.violet", "city.newnan.violet_v2_1_0")
    // helper
    relocate("me.lucko.helper", "me.lucko.helper_v5_6_14")
    relocate("me.lucko.helper", "me.lucko.helper_v5_6_14")
    relocate("me.lucko.shadow", "me.lucko.shadow_v5_6_14")
    // jackson
    relocate("org.yaml.snakeyaml", "org.yaml.snakeyaml_v2_0")
    relocate("com.fasterxml.jackson", "com.fasterxml.jackson_v2_15_2")
    relocate("com.jasonclawson.jackson", "com.jasonclawson.jackson_v1_1_0")
    // HikariCP + ktorm
    relocate("com.zaxxer.hikari", "com.zaxxer.hikari_v4_0_3")
    relocate("org.ktorm", "org.ktorm_v3_6_0")
}

dependencies {
    // API
    // https://github.com/ElgarL/GroupManager
    listOf("com.github.ElgarL:groupmanager:2.9").forEach { compileOnly(it); testImplementation(it) }

    // Utils
    implementation("me.lucko:helper:5.6.14")
    implementation("com.github.NewNanCity:Violet:2.1.0")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")

    // mysql
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("org.ktorm:ktorm-core:3.6.0")
}
version = "0.0.1"
group = "city.newnan.feefly"
description = "Pay for Fly"
project.ext["depend"] = listOf("Vault")
project.ext["loadBefore"] = listOf("Vault")

tasks.shadowJar {
    // Violet
    relocate("city.newnan.violet", "city.newnan.violet_v2_1_0")

    // helper
    relocate("me.lucko.helper", "me.lucko.helper_v5_6_14")
    relocate("me.lucko.helper", "me.lucko.helper_v5_6_14")
    relocate("me.lucko.shadow", "me.lucko.shadow_v5_6_14")

    // commands
    relocate("co.aikar.locales", "co.aikar.locales_v0_5_1")
    relocate("co.aikar.commands", "co.aikar.commands_v0_5_1")

    // jackson
    relocate("org.yaml.snakeyaml", "org.yaml.snakeyaml_v2_0")
    relocate("com.fasterxml.jackson", "com.fasterxml.jackson_v2_15_2")
}

dependencies {
    // API
    listOf("com.github.MilkBowl:VaultAPI:1.7.1").forEach { compileOnly(it); testImplementation(it) }

    // Utils
    implementation("me.lucko:helper:5.6.14")
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation("com.github.NewNanCity:Violet:2.1.0")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
}

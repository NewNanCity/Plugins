version = "0.0.1"
group = "city.newnan.railarea"
description = "Rail area mention"
project.ext["softDepend"] = listOf("WorldEdit")
project.ext["loadBefore"] = listOf("WorldEdit")

dependencies {
    // API
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.0-SNAPSHOT")

    // Utils
    implementation("me.lucko:helper:5.6.14")
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation("dev.triumphteam:triumph-gui:3.1.2")
    implementation("com.github.NewNanCity:Violet:2.1.0")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
}
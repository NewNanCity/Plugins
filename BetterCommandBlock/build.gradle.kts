version = "0.0.1"
group = "city.newnan.bettercommandblock"
description = "Better Command Block"

dependencies {
    compileOnly("net.essentialsx:EssentialsX:2.19.7")

    // Utils
    implementation("me.lucko:helper:5.6.14")
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation("com.github.NewNanCity:Violet:2.1.5")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
}

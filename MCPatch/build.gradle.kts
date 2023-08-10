version = "0.0.1"
group = "city.newnan.mcpatch"
description = "A Patch for Minecraft Server."


tasks.shadowJar {
    // Violet
    relocate("city.newnan.violet", "city.newnan.violet_v2_1_0")

    // helper
    relocate("me.lucko.helper", "me.lucko.helper_v5_6_14")
    relocate("me.lucko.helper", "me.lucko.helper_v5_6_14")
    relocate("me.lucko.shadow", "me.lucko.shadow_v5_6_14")
}

dependencies {
    // Utils
    implementation("me.lucko:helper:5.6.14")
    implementation("com.github.NewNanCity:Violet:2.1.0")
}
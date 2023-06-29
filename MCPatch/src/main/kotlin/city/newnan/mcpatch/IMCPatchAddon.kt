package city.newnan.mcpatch

import me.lucko.helper.terminable.Terminable

interface IMCPatchAddon : Terminable {
    val addonName: String
    fun enable()
}
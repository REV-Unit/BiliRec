package moe.peanutmelonseedbigalmond.bilirec.config

import com.esotericsoftware.yamlbeans.YamlConfig
import com.esotericsoftware.yamlbeans.YamlWriter
import java.io.File
import java.io.FileWriter

object ConfigWriter {
    fun write(config: ConfigRoot, file: File) {
        val writer = YamlWriter(FileWriter(file))
        writer.config.setPropertyElementType(ConfigRoot::class.java, "roomConfigs", RoomConfig::class.java)
        writer.config.writeConfig.setWriteClassname(YamlConfig.WriteClassName.NEVER)
        writer.write(config)
        writer.close()
    }
}
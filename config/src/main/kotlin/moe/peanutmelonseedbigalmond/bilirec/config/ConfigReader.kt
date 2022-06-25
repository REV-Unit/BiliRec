package moe.peanutmelonseedbigalmond.bilirec.config

import com.esotericsoftware.yamlbeans.YamlReader
import java.io.File
import java.io.FileReader

object ConfigReader {
    fun readConfig(file: File): ConfigRoot {
        val yamlReader = YamlReader(FileReader(file))
        val configRoot = yamlReader.read(ConfigRoot::class.java)
        yamlReader.close()
        return configRoot
    }
}
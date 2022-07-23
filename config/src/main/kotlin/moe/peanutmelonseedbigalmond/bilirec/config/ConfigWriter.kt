package moe.peanutmelonseedbigalmond.bilirec.config

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.encodeToStream
import java.io.File

object ConfigWriter {
    private val yamlConfig = YamlConfiguration()
    private val yaml = Yaml(configuration = yamlConfig)
    fun write(config: ConfigRoot, file: File) {
        file.outputStream().use {
            yaml.encodeToStream(config, it)
        }
    }
}
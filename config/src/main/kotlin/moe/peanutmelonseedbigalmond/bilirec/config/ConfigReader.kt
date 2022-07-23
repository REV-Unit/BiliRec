package moe.peanutmelonseedbigalmond.bilirec.config

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import java.io.File

object ConfigReader {
    fun readConfig(file: File): ConfigRoot {
        return file.inputStream().use {
            Yaml.default.decodeFromStream(it)
        }
    }
}
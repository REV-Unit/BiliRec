package moe.peanutmelonseedbigalmond.bilirec.app

import kotlinx.coroutines.runBlocking
import moe.peanutmelonseedbigalmond.bilirec.config.ConfigRoot
import moe.peanutmelonseedbigalmond.bilirec.logging.LoggingFactory
import moe.peanutmelonseedbigalmond.bilirec.recording.Recording
import moe.peanutmelonseedbigalmond.bilirec.recording.Room
import org.apache.commons.cli.*
import java.io.File
import kotlin.coroutines.coroutineContext

class CommandProcessor(args: Array<String>) {
    private lateinit var cli: CommandLine
    private val cliParser = DefaultParser()
    private val helpFormatter = HelpFormatter()
    val options = Options().also {
        it.addOption("h", "help", false, "Print this help message")
        it.addOption(
            Option.builder()
                .option("c")
                .longOpt("config-path")
                .desc("Specify the path to the config file")
                .hasArg(true)
                .required(true)
                .build()
        )
    }

    init {
        try {
            cli = cliParser.parse(options, args)
        } catch (e: ParseException) {
            helpFormatter.printHelp("bilirec", options)
        }
    }

    suspend fun processAsync() {
        if (cli.hasOption("h")) {
            helpFormatter.printHelp("bilirec", options)
            return
        }
        if (cli.hasOption("c")) {
            val configPath = cli.getOptionValue("c")
            val configFile = File(configPath, ConfigProcessor.DEFAULT_CONFIG_FILE_NAME)
            val configRoot = ConfigProcessor.loadConfig(configFile)
            appStart(configRoot)
            return
        }

        helpFormatter.printHelp("bilirec", options)
    }

    private suspend fun appStart(configRoot: ConfigRoot) {
        val rooms = configRoot.roomConfigs ?: emptyList()
        if (rooms.isEmpty()) {
            LoggingFactory.getLogger().warn("No rooms configured")
            return
        }
        for (roomConfig in rooms) {
            val room = Room(roomConfig, coroutineContext)
            Recording.INSTANCE.registerTaskAsync(room)
        }

        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                runBlocking { Recording.INSTANCE.unregisterAllTasksAsync() }
                LoggingFactory.getLogger().info("System Exited")
            }
        })
    }
}
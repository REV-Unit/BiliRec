package moe.peanutmelonseedbigalmond.bilirec

import kotlinx.coroutines.runBlocking
import moe.peanutmelonseedbigalmond.bilirec.app.CommandProcessor
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class BiliRecApplication

fun main(args: Array<String>) = runBlocking {
    SpringApplication.run(BiliRecApplication::class.java,*args)
    CommandProcessor.processAsync(args)
}
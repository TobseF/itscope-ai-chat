package com.example.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = ["com.example.app", "de.itscope"],
)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
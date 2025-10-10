package de.itscope.ai.mcp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
class ITscopeMcpApplication

fun main(args: Array<String>) {
	runApplication<ITscopeMcpApplication>(*args)
}

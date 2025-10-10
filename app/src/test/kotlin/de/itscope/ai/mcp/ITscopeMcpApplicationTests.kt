package de.itscope.ai.mcp

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@Import(TestcontainersConfiguration::class)
@SpringBootTest
class ITscopeMcpApplicationTests {

	@Test
	fun contextLoads() {
	}

}

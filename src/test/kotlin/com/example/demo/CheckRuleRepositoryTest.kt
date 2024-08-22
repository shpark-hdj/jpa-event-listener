package com.example.demo

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class CheckRuleRepositoryTest {
    @Autowired
    private lateinit var checkRuleRepository: CheckRuleRepository

    @Test
    fun checkRuleCRUDTest() {
        checkRuleRepository.saveAll(
            listOf(
                CheckRule(
                    checkRuleId = "rule1",
                    ruleName = "rule 수정",
                    ruleContent = "testtttt"
                ),
                CheckRule(
                    checkRuleId = "rule5",
                    ruleContent = "test5"
                )
            )
        )
        checkRuleRepository.deleteById("rule2")
    }
}

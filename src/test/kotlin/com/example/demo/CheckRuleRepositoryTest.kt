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

    @Test
    fun `Id 생성 전략을 사용하지 않았을 때 insert 확인`() {
        // bulk insert 안되고 단건 insert 쿼리로 실행됨
        checkRuleRepository.saveAll(listOf(
            CheckRule(
                checkRuleId = "rule10",
                ruleContent = "test10"
            ),
            CheckRule(
                checkRuleId = "rule11",
                ruleContent = "test11"
            ),
            CheckRule(
                checkRuleId = "rule12",
                ruleContent = "test12"
            ),
            CheckRule(
                checkRuleId = "rule13",
                ruleContent = "test13"
            ),
            CheckRule(
                checkRuleId = "rule14",
                ruleContent = "test14"
            )
        ))
    }
}

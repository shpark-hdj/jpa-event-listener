package com.example.demo

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(rollbackFor = [Exception::class])
class CheckRuleService(private val checkRuleRepository: CheckRuleRepository) {
}

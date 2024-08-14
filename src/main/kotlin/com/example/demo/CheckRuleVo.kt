package com.example.demo

data class CheckRuleVo(
    val checkRuleId: String,
    val historyType: HistoryType,
    val ruleContent: String,
    val ruleName: String? = null,
    val description: String? = null
)

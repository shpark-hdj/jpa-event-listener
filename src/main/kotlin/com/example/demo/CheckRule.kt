package com.example.demo

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.Id

@EntityListeners(CheckRuleListener::class)
@Entity
class CheckRule(
    @Id
    val checkRuleId: String,
    val ruleContent: String,
    val ruleName: String? = null,
    val description: String? = null,
) {
    @Column(insertable = false, updatable = false)
    val createDtm: LocalDateTime? = null

    @Column(insertable = false, updatable = false)
    val updateDtm: LocalDateTime? = null
}

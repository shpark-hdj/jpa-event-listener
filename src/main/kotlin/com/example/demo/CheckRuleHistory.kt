package com.example.demo

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType.STRING
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
class CheckRuleHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val checkRuleHistoryNid: Long? = null,
    val checkRuleId: String,
    @Enumerated(STRING)
    val historyTypeCode: HistoryType,
    val checkRuleContent: String,
) {
    @Column(insertable = false, updatable = false)
    val createDtm: LocalDateTime? = null
    @Column(insertable = false, updatable = false)
    val updateDtm: LocalDateTime? = null
}

enum class HistoryType {
    SAVE, UPDATE, DELETE
}

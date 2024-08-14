package com.example.demo

import org.springframework.data.jpa.repository.JpaRepository

interface CheckRuleRepository: JpaRepository<CheckRule, Long>

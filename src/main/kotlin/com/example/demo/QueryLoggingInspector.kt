package com.example.demo

import javax.persistence.EntityManagerFactory
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.hibernate.event.service.spi.EventListenerRegistry
import org.hibernate.event.spi.EventType
import org.hibernate.event.spi.PreDeleteEvent
import org.hibernate.event.spi.PreInsertEvent
import org.hibernate.event.spi.PreUpdateEvent
import org.hibernate.persister.entity.EntityPersister
import org.hibernate.resource.jdbc.spi.StatementInspector
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

// inspector 를 쿼리를 뽑아낼 때 사용
@Component
class QueryLoggingInspector : StatementInspector {

    companion object {
        private val threadLocalParams = ThreadLocal<MutableList<Pair<Map<String, Any?>, EntityPersister>>>()

        fun getAndClearParams(): List<Pair<Map<String, Any?>, EntityPersister>>? {
            val params = threadLocalParams.get()
            threadLocalParams.remove()
            return params
        }

        fun addParams(params: Pair<Map<String, Any?>, EntityPersister>) {
            val currentParams = threadLocalParams.get() ?: mutableListOf()
            currentParams.add(params)
            threadLocalParams.set(currentParams)
        }
    }

    private val auditedEntities = setOf(CheckRule::class)

    @Autowired
    fun init(entityManagerFactory: EntityManagerFactory) {
        val sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor::class.java)
        val registry = sessionFactory.serviceRegistry.getService(EventListenerRegistry::class.java)

        registry.getEventListenerGroup(EventType.PRE_INSERT).appendListener(PreInsertEventListener())
        registry.getEventListenerGroup(EventType.PRE_UPDATE).appendListener(PreUpdateEventListener())
        registry.getEventListenerGroup(EventType.PRE_DELETE).appendListener(PreDeleteEventListener())
    }

    override fun inspect(sql: String?): String? {
        if (sql == null || sql.lowercase().startsWith("select")) return sql

        val params = getAndClearParams()
        if (!params.isNullOrEmpty()) {
            val (parameterMap, persister) = params[0]
            val bindedSql = bindParameters(sql, parameterMap, persister)
            logSql(bindedSql, parameterMap)
        }

        return sql
    }

    private fun bindParameters(sql: String, parameterMap: Map<String, Any?>, persister: EntityPersister): String {
        return when {
            sql.startsWith("/* insert") -> bindInsertParameters(sql, parameterMap, persister)
            sql.startsWith("/* update") -> bindUpdateParameters(sql, parameterMap, persister)
            sql.startsWith("/* delete") -> bindDeleteParameters(sql, parameterMap, persister)
            else -> sql
        }
    }

    private fun bindInsertParameters(sql: String, parameterMap: Map<String, Any?>, persister: EntityPersister): String {
        val insertableProperties = persister.propertyNames
            .filterIndexed { index, _ -> persister.propertyInsertability[index] }
        val columns = insertableProperties + (persister.identifierPropertyName ?: "")
        val values = columns.map { formatParameter(parameterMap[it]) }

        val columnsString = columns.joinToString(", ")
        val valuesString = values.joinToString(", ")

        return sql.replaceFirst(Regex("\\([^)]*\\) values \\([^)]*\\)"), "($columnsString) values ($valuesString)")
    }

    private fun bindUpdateParameters(sql: String, parameterMap: Map<String, Any?>, persister: EntityPersister): String {
        val updatableProperties = persister.propertyNames
            .filterIndexed { index, _ -> persister.propertyUpdateability[index] }

        val setClause = updatableProperties
            .filter { it != persister.identifierPropertyName }
            .joinToString(", ") { "$it=${formatParameter(parameterMap[it])}" }

        val whereClause = "${persister.identifierPropertyName}=${formatParameter(parameterMap[persister.identifierPropertyName])}"

        return sql.replaceFirst(Regex("set [^w]*where [^)]*"), "set $setClause where $whereClause")
    }

    private fun bindDeleteParameters(sql: String, parameterMap: Map<String, Any?>, persister: EntityPersister): String {
        val idPropertyName = persister.identifierPropertyName
        val idValue = formatParameter(parameterMap[idPropertyName])
        return sql.replace("?", idValue)
    }

    private fun formatParameter(param: Any?): String {
        return when (param) {
            null -> "NULL"
            is String -> "'$param'"
            is Number -> param.toString()
            is Boolean -> if (param) "1" else "0"
            else -> "'$param'"
        }
    }

    private fun logSql(sql: String, params: Map<String, Any?>) {
        println("Executed SQL: $sql")
        println("Parameters: $params")
    }

    inner class PreInsertEventListener : org.hibernate.event.spi.PreInsertEventListener {
        override fun onPreInsert(event: PreInsertEvent): Boolean {
            if (event.entity::class in auditedEntities) {
                val paramMap = createParamMap(event.persister, event.state, event.id)
                addParams(Pair(paramMap, event.persister))
            }
            return false
        }
    }

    inner class PreUpdateEventListener : org.hibernate.event.spi.PreUpdateEventListener {
        override fun onPreUpdate(event: PreUpdateEvent): Boolean {
            if (event.entity::class in auditedEntities) {
                val paramMap = createParamMap(event.persister, event.state, event.id)
                addParams(Pair(paramMap, event.persister))
            }
            return false
        }
    }

    inner class PreDeleteEventListener : org.hibernate.event.spi.PreDeleteEventListener {
        override fun onPreDelete(event: PreDeleteEvent): Boolean {
            if (event.entity::class in auditedEntities) {
                val paramMap = createParamMap(event.persister, event.deletedState, event.id)
                addParams(Pair(paramMap, event.persister))
            }
            return false
        }
    }

    private fun createParamMap(persister: EntityPersister, state: Array<Any?>, id: Any?): Map<String, Any?> {
        val paramMap = mutableMapOf<String, Any?>()
        val propertyNames = persister.propertyNames

        for (i in propertyNames.indices) {
            paramMap[propertyNames[i]] = state[i]
        }

        val idPropertyName = persister.identifierPropertyName
        if (idPropertyName != null) {
            paramMap[idPropertyName] = id
        }

        return paramMap
    }
}

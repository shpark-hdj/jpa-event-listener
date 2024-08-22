package com.example.demo

import javax.persistence.EntityManager
import javax.persistence.PrePersist
import javax.persistence.PreRemove
import javax.persistence.PreUpdate
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import org.hibernate.engine.spi.SessionImplementor
import org.hibernate.persister.entity.EntityPersister
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component

@Component
class CheckRuleEventListener : ApplicationContextAware {

    private lateinit var applicationContext: ApplicationContext

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

    private val entityManager: EntityManager by lazy {
        applicationContext.getBean(EntityManager::class.java)
    }

    @PrePersist
    fun onPrePersist(entity: Any) {
        logSql("INSERT", entity)
    }

    @PreUpdate
    fun onPreUpdate(entity: Any) {
        logSql("UPDATE", entity)
    }

    @PreRemove
    fun onPreRemove(entity: Any) {
        logSql("DELETE", entity)
    }

    private fun logSql(operation: String, entity: Any) {
        val session = entityManager.unwrap(SessionImplementor::class.java)
        val persister = session.factory.metamodel.entityPersister(entity.javaClass)

        val (sql, params) = when (operation) {
            "INSERT" -> generateInsertSql(persister, entity)
            "UPDATE" -> generateUpdateSql(persister, entity)
            "DELETE" -> generateDeleteSql(persister, entity)
            else -> throw IllegalArgumentException("Unsupported operation: $operation")
        }

        println("Executed SQL: $sql")
        println("Parameters: $params")
    }

    private fun generateInsertSql(persister: EntityPersister, entity: Any): Pair<String, Map<String, Any?>> {
        val tableName = getTableName(persister)
        val insertableProperties = persister.propertyNames
            .filterIndexed { index, _ -> persister.propertyInsertability[index] }
        val columns = (listOf(persister.identifierPropertyName) + insertableProperties).joinToString(", ") { it.toSnakeCase() }
        val params = getEntityState(entity, insertableProperties)

        val values = (listOf(entityManager.entityManagerFactory.persistenceUnitUtil.getIdentifier(entity)) + insertableProperties.map { params[it] }).joinToString { formatValue(it) }
        val sql = "INSERT INTO $tableName ($columns) VALUE ($values);"

        return sql to params
    }

    private fun generateUpdateSql(persister: EntityPersister, entity: Any): Pair<String, Map<String, Any?>> {
        val tableName = getTableName(persister)
        val updatableProperties = persister.propertyNames
            .filterIndexed { index, _ -> persister.propertyUpdateability[index] }
        val params = getEntityState(entity, updatableProperties + persister.identifierPropertyName)

        val setClauses = updatableProperties
            .filter { params[it] != null }
            .joinToString(", ") {
                "${it.toSnakeCase()} = ${formatValue(params[it])}"
            }

        val idName = persister.identifierPropertyName
        val idValue = params[idName]

        val sql = "UPDATE $tableName SET $setClauses WHERE ${idName.toSnakeCase()} = ${formatValue(idValue)};"

        return sql to params.filterValues { it != null }
    }

    private fun generateDeleteSql(persister: EntityPersister, entity: Any): Pair<String, Map<String, Any?>> {
        val tableName = getTableName(persister)
        val idName = persister.identifierPropertyName
        val params = getEntityState(entity, listOf(idName))
        val idValue = params[idName]
        val sql = "DELETE FROM $tableName WHERE ${idName.toSnakeCase()} = ${formatValue(idValue)};"
        return sql to params
    }

    private fun getTableName(persister: EntityPersister): String {
        return persister.entityName.substringAfterLast('.').toSnakeCase()
    }

    private fun getEntityState(entity: Any, properties: List<String>): Map<String, Any?> {
        return properties.associateWith { prop -> getPropertyValueSafely(entity, prop) }
    }

    private fun getPropertyValueSafely(entity: Any, propertyName: String): Any? {
        return try {
            val property = entity::class.memberProperties.find { it.name == propertyName }
            property?.let {
                it.isAccessible = true
                it.getter.call(entity)
            }
        } catch (e: Exception) {
            println("Error getting property $propertyName: ${e.message}")
            null
        }
    }

    private fun String.toSnakeCase(): String {
        return replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
    }

    private fun formatValue(value: Any?): String {
        return when (value) {
            null -> "null"
            is String -> "'$value'"
            is Number -> value.toString()
            is Boolean -> if (value) "1" else "0"
            else -> "'$value'"
        }
    }
}

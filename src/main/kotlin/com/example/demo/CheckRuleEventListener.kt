package com.example.demo

import com.example.demo.HistoryType.DELETE
import com.example.demo.HistoryType.SAVE
import com.example.demo.HistoryType.UPDATE
import javax.persistence.EntityManagerFactory
import javax.persistence.PrePersist
import javax.persistence.PreRemove
import javax.persistence.PreUpdate
import org.hibernate.engine.spi.SessionImplementor
import org.hibernate.persister.entity.EntityPersister
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component
class CheckRuleEventListener {

    @Autowired
    @Lazy
    private lateinit var entityManagerFactory: EntityManagerFactory

    @PrePersist
    fun onPrePersist(entity: Any) {
        generateSql(SAVE, entity)
    }

    @PreUpdate
    fun onPreUpdate(entity: Any) {
        generateSql(UPDATE, entity)
    }

    @PreRemove
    fun onPreRemove(entity: Any) {
        generateSql(DELETE, entity)
    }

    private fun generateSql(historyType: HistoryType, entity: Any) {
        val entityMetadata = createEntityMetadata(entity)
        val sql = when (historyType) {
            SAVE -> generateInsertSql(entityMetadata, entity)
            UPDATE -> generateUpdateSql(entityMetadata, entity)
            DELETE -> generateDeleteSql(entityMetadata, entity)
        }
        println("Executed SQL: $sql")
    }

    private fun generateInsertSql(entityMetadata: EntityMetadata, entity: Any): String {
        val columns = entityMetadata.insertableProperties.joinToString(", ") { it.toSnakeCase() }
        val params = getEntityPropertyMap(entity, entityMetadata.insertableProperties, entityMetadata.persister)
        println("Parameters: $params")
        val values = entityMetadata.insertableProperties.map { params[it] }.joinToString { formatValue(it) }
        val sql = "INSERT INTO ${entityMetadata.tableName} ($columns) VALUE ($values);"

        return sql
    }

    private fun generateUpdateSql(entityMetadata: EntityMetadata, entity: Any): String {
        val params = getEntityPropertyMap(entity, entityMetadata.updatableProperties, entityMetadata.persister)
        println("Parameters: $params")
        // 파라미터로 오지 않는 컬럼은 업데이트 하지 않도록 필터
        val setClauses = entityMetadata.updatableProperties
            .filter { params[it] != null && it != entityMetadata.idProperty }
            .joinToString(", ") {
                "${it.toSnakeCase()} = ${formatValue(params[it])}"
            }
        val idValue = params[entityMetadata.idProperty]
        val sql = "UPDATE ${entityMetadata.tableName} SET $setClauses WHERE ${entityMetadata.idProperty.toSnakeCase()} = ${formatValue(idValue)};"

        return sql
    }

    private fun generateDeleteSql(entityMetadata: EntityMetadata, entity: Any): String {
        val params = getEntityPropertyMap(entity, listOf(entityMetadata.idProperty), entityMetadata.persister)
        println("Parameters: $params")
        val idValue = params[entityMetadata.idProperty]
        val sql = "DELETE FROM ${entityMetadata.tableName} WHERE ${entityMetadata.idProperty.toSnakeCase()} = ${formatValue(idValue)};"

        return sql
    }

    private fun getEntityPropertyMap(entity: Any, properties: List<String>, persister: EntityPersister): Map<String, Any?> {
        val idValue = persister.getIdentifier(entity, null)
        val idProperty = persister.identifierPropertyName

        return buildMap {
            put(idProperty, idValue)
            properties
                .filter { it != idProperty }
                .forEach { prop ->
                put(prop, persister.getPropertyValue(entity, prop))
            }
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
            is Enum<*> -> "'${value.name}'"
            else -> "'$value'"
        }
    }

    private fun createEntityMetadata(entity: Any): EntityMetadata {
        val session = entityManagerFactory.createEntityManager().unwrap(SessionImplementor::class.java)
        val persister = session.factory.metamodel.entityPersister(entity::class.java) as EntityPersister
        val idProperty = persister.identifierPropertyName
        return EntityMetadata(
            persister = persister,
            // entityName 은 패키지를 포함하기 때문에 마지막만 잘라서 처리
            tableName = persister.entityName.substringAfterLast('.').toSnakeCase(),
            idProperty = idProperty,
            // 엔티티에 insertable, updatable 이 false 인 값들은 제외
            insertableProperties = getFilteredProperties(persister, idProperty) { it.propertyInsertability },
            updatableProperties = getFilteredProperties(persister, idProperty) { it.propertyUpdateability }
        )
    }

    private fun getFilteredProperties(
        persister: EntityPersister,
        idProperty: String,
        filterPredicate: (EntityPersister) -> BooleanArray,
    ): List<String> {
        return buildList {
            add(idProperty)
            addAll(persister.propertyNames.filterIndexed { index, _ ->
                filterPredicate(persister)[index]
            })
        }
    }
}

data class EntityMetadata(
    val persister: EntityPersister,
    val tableName: String,
    val idProperty: String,
    val insertableProperties: List<String>,
    val updatableProperties: List<String>,
)

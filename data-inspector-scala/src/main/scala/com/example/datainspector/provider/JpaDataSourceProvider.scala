/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package com.example.datainspector.provider

import com.example.datainspector.model.{DataSourceInfo, QueryResult}
import com.example.datainspector.spi.DataSourceProvider
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.{EntityManager, EntityManagerFactory, TypedQuery}
import jakarta.persistence.metamodel.EntityType
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.stereotype.Component

import java.util.Optional
import scala.jdk.CollectionConverters._
import scala.util.{Try, Using}

/**
 * Provides JPA entity data sources for SQL databases
 * Discovers all JPA entities and allows querying them
 */
@Component
class JpaDataSourceProvider(
  entityManagerFactoryOpt: Optional[EntityManagerFactory],
  objectMapper: ObjectMapper
) extends DataSourceProvider {

  private val logger: Logger = LoggerFactory.getLogger(classOf[JpaDataSourceProvider])
  private val entityManagerFactory: Option[EntityManagerFactory] =
    if (entityManagerFactoryOpt.isPresent) Some(entityManagerFactoryOpt.get()) else None

  if (entityManagerFactory.isDefined) {
    logger.info("JPA data source provider enabled")
  }

  override def discoverDataSources(): List[DataSourceInfo] = {
    entityManagerFactory match {
      case None => List.empty
      case Some(emf) =>
        Try {
          val entities = emf.getMetamodel.getEntities.asScala.toSet
          val em = emf.createEntityManager()

          try {
            val dataSources = scala.collection.mutable.ListBuffer[DataSourceInfo]()

            // Add database overview
            dataSources += DataSourceInfo(
              id = "jpa:overview",
              name = "Database Overview",
              `type` = "jpa-overview",
              description = "Overview of all JPA entities and database statistics",
              size = entities.size,
              queryable = true
            )

            // Add each entity as a data source
            entities.foreach { entity =>
              val entityName = entity.getName
              val javaType = entity.getJavaType

              Try {
                val count = getEntityCount(em, entityName)

                dataSources += DataSourceInfo(
                  id = s"jpa:entity:$entityName",
                  name = s"Entity: $entityName",
                  `type` = "jpa-entity",
                  description = s"JPA Entity: ${javaType.getName}",
                  size = count,
                  queryable = true,
                  metadata = Map(
                    "entityName" -> entityName,
                    "javaType" -> javaType.getName,
                    "tableName" -> getTableName(entity)
                  )
                )
              }.recover {
                case e: Exception =>
                  logger.debug(s"Could not get count for entity $entityName: ${e.getMessage}")
              }
            }

            dataSources.toList
          } finally {
            em.close()
          }
        }.recover {
          case e: Exception =>
            logger.warn(s"JPA is not available or no entities found: ${e.getMessage}")
            List.empty[DataSourceInfo]
        }.getOrElse(List.empty)
    }
  }

  override def query(dataSourceId: String, filters: Map[String, AnyRef], limit: Int, offset: Int): QueryResult = {
    if (dataSourceId == "jpa:overview") {
      queryOverview()
    } else if (dataSourceId.startsWith("jpa:entity:")) {
      val entityName = dataSourceId.substring("jpa:entity:".length)
      queryEntity(entityName, filters, limit, offset)
    } else {
      QueryResult(
        dataSourceId = dataSourceId,
        data = List.empty,
        totalCount = 0,
        limit = limit,
        offset = offset
      )
    }
  }

  override def supports(dataSourceId: String): Boolean = {
    dataSourceId != null && dataSourceId.startsWith("jpa:")
  }

  private def getEntityCount(em: EntityManager, entityName: String): Long = {
    val query = em.createQuery(s"SELECT COUNT(e) FROM $entityName e")
    query.getSingleResult.asInstanceOf[Number].longValue()
  }

  private def getTableName(entity: EntityType[_]): String = {
    Try {
      val table = entity.getJavaType.getAnnotation(classOf[jakarta.persistence.Table])
      if (table != null && table.name().nonEmpty) {
        table.name()
      } else {
        entity.getName
      }
    }.getOrElse(entity.getName)
  }

  private def queryOverview(): QueryResult = {
    entityManagerFactory match {
      case None => QueryResult("jpa:overview", List.empty, 0, 0, 0)
      case Some(emf) =>
        val em = emf.createEntityManager()
        try {
          val entities = emf.getMetamodel.getEntities.asScala

          val data = entities.flatMap { entity =>
            val entityName = entity.getName
            Try {
              val count = getEntityCount(em, entityName)
              Map[String, AnyRef](
                "entityName" -> entityName,
                "javaType" -> entity.getJavaType.getName,
                "tableName" -> getTableName(entity),
                "recordCount" -> Long.box(count),
                "attributes" -> Int.box(entity.getAttributes.size())
              )
            }.recover {
              case e: Exception =>
                logger.debug(s"Error querying entity $entityName: ${e.getMessage}")
                null
            }.toOption
          }.filter(_ != null).toList

          QueryResult(
            dataSourceId = "jpa:overview",
            data = data,
            totalCount = data.size,
            limit = data.size,
            offset = 0
          )
        } finally {
          em.close()
        }
    }
  }

  private def queryEntity(entityName: String, filters: Map[String, AnyRef], limit: Int, offset: Int): QueryResult = {
    entityManagerFactory match {
      case None => QueryResult(s"jpa:entity:$entityName", List.empty, 0, limit, offset)
      case Some(emf) =>
        val em = emf.createEntityManager()
        try {
          // Get total count
          val totalCount = getEntityCount(em, entityName)

          // Build query with filters
          val jpql = new StringBuilder(s"SELECT e FROM $entityName e")

          if (filters.nonEmpty) {
            jpql.append(" WHERE ")
            val conditions = filters.keys.map(key => s"e.$key = :$key").mkString(" AND ")
            jpql.append(conditions)
          }

          val query: TypedQuery[AnyRef] = em.createQuery(jpql.toString(), classOf[AnyRef])

          // Set filter parameters
          filters.foreach { case (key, value) =>
            query.setParameter(key, value)
          }

          // Apply pagination
          query.setFirstResult(offset)
          query.setMaxResults(limit)

          val results = query.getResultList.asScala.toList

          // Convert entities to maps
          val data = results.map(convertEntityToMap)

          QueryResult(
            dataSourceId = s"jpa:entity:$entityName",
            data = data,
            totalCount = totalCount,
            limit = limit,
            offset = offset,
            stats = Map[String, AnyRef]("entityName" -> entityName)
          )
        } catch {
          case e: Exception =>
            logger.error(s"Error querying entity $entityName: ${e.getMessage}", e)
            QueryResult(
              dataSourceId = s"jpa:entity:$entityName",
              data = List.empty,
              totalCount = 0,
              limit = limit,
              offset = offset
            )
        } finally {
          em.close()
        }
    }
  }

  private def convertEntityToMap(entity: AnyRef): Map[String, AnyRef] = {
    Try {
      // Use ObjectMapper to convert entity to map
      val javaMap = objectMapper.convertValue(entity, classOf[java.util.Map[String, AnyRef]])
      javaMap.asScala.toMap
    }.recover {
      case e: Exception =>
        logger.debug(s"Could not convert entity to map: ${e.getMessage}")
        Map[String, AnyRef](
          "type" -> entity.getClass.getSimpleName,
          "value" -> entity.toString
        )
    }.get
  }
}

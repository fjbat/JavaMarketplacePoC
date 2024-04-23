package com.dtme.marketplace.helper;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.RequestContext;

import com.dtme.marketplace.entities.VendureEntity;
import com.dtme.marketplace.exceptions.UserInputException;
import com.dtme.marketplace.helper.entries.ExtendedListQueryOptions;
import com.dtme.marketplace.helper.entries.FilterParameter;
import com.dtme.marketplace.helper.entries.ListQueryOptions;
import com.dtme.marketplace.helper.entries.LogicalOperator;
import com.dtme.marketplace.helper.entries.PaginationParams;
import com.dtme.marketplace.helper.entries.RelationMetadata;
import com.dtme.marketplace.helper.entries.TakeSkipParams;
import com.dtme.marketplace.helper.entries.TransactionalConnection;
import com.dtme.marketplace.helper.entries.WhereGroup;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ListQueryBuilder<T extends VendureEntity> {

	private final TransactionalConnection transactionalConnection;
	private final ConfigService configService;
	private final Logger logger;
	@Autowired
	private final EntityManager entityManager;
//    
//    public YourRepository(EntityManager entityManager) {
//    	this.entityManager = entityManager;
//    }

	@Autowired
	public ListQueryBuilder(TransactionalConnection transactionalConnection, ConfigService configService,
			Logger logger) {
		this.transactionalConnection = new TransactionalConnection();
		// this.transactionalConnection = transactionalConnection;
		this.configService = configService;
		this.logger = logger;
		this.entityManager = null;
	}

	@Transactional
	public List<T> build(Class<T> entityClass, ExtendedListQueryOptions<T> options) {
		// Implement the logic to build the query and execute it using Spring Data JPA
		// or native queries
		return null;
	}

	public <FP extends FilterParameter<VendureEntity>> boolean filterObjectHasProperty(FP filterObject,
			String property) {
		if (filterObject == null) {
			return false;
		}
		for (String key : filterObject.keySet()) {
			Object value = filterObject.get(key);
			if (value == null) {
				continue;
			}
			if (key.equals(property)) {
				return true;
			}
			if (key.equals("_and") || key.equals("_or")) {
				@SuppressWarnings("unchecked")
				Iterable<FP> conditions = (Iterable<FP>) value;
				for (FP condition : conditions) {
					if (filterObjectHasProperty(condition, property)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public <T extends VendureEntity> List<T> build(Class<T> entityClass, ListQueryOptions<T> options,
			ExtendedListQueryOptions<T> extendedOptions) {
		String apiType = extendedOptions.getCtx() != null ? extendedOptions.getCtx().getApiType() : "shop";
		int take = parseTakeSkipParams(apiType, options, extendedOptions.isIgnoreQueryLimits()).getTake();
		int skip = parseTakeSkipParams(apiType, options, extendedOptions.isIgnoreQueryLimits()).getSkip();

		String alias = extendedOptions.getEntityAlias() != null ? extendedOptions.getEntityAlias()
				: entityClass.getSimpleName().toLowerCase();
		List<String> minimumRequiredRelations = getMinimumRequiredRelations(entityClass, options, extendedOptions);

		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(entityClass);
		Root<T> root = criteriaQuery.from(entityClass);

		List<String> relations = new ArrayList<>(minimumRequiredRelations);
		if (extendedOptions.getRelations() != null) {
			relations.addAll(extendedOptions.getRelations());
		}

		// Special case for the 'collection' entity
		// This is bypassed an issue in TypeORM where it would join the same relation
		// multiple times.
		// See https://github.com/typeorm/typeorm/issues/9936 for more context.
		List<String> processedRelations = joinTreeRelationsDynamically(entityClass, relations);

		// Remove any relations which are related to the 'collection' tree, as these are
		// handled separately
		// to avoid duplicate joins.
		relations.removeAll(processedRelations);

		// Apply relations
		for (String relation : relations) {
			root.join(relation, JoinType.INNER);
		}

		// Apply where clause
		if (extendedOptions.getWhere() != null) {
			Specification<T> spec = (root, query, cb) -> {
				// Your logic to construct the Specification based on extendedOptions.getWhere()
			};
			criteriaQuery.where(spec.toPredicate(root, criteriaQuery, criteriaBuilder));
		}

		// Apply sorting
		// Your logic to apply sorting based on options.getSort() and
		// extendedOptions.getOrderBy()

		TypedQuery<T> typedQuery = entityManager.createQuery(criteriaQuery);
		typedQuery.setFirstResult(skip);
		typedQuery.setMaxResults(take);

		return typedQuery.getResultList();
	}
	public PaginationParams parseTakeSkipParams(ApiType apiType, ListQueryOptions<?> options, boolean ignoreQueryLimits) {
        int shopListQueryLimit = configService.getApiOptions().getShopListQueryLimit();
        int adminListQueryLimit = configService.getApiOptions().getAdminListQueryLimit();
        int takeLimit = ignoreQueryLimits ? Integer.MAX_VALUE : (apiType == ApiType.ADMIN ? adminListQueryLimit : shopListQueryLimit);

        int take = options.getTake() != null ? Math.min(Math.max(options.getTake(), 0), takeLimit) : takeLimit;
        int skip = options.getSkip() != null ? Math.max(options.getSkip(), 0) : 0;

        if (options.getTake() != null && options.getTake() > takeLimit) {
            throw new UserInputException("error.list-query-limit-exceeded", Collections.singletonMap("limit", takeLimit));
        }

        return new PaginationParams(take, skip);
    }

	private void addNestedWhereClause(CriteriaBuilder criteriaBuilder, CriteriaQuery<?> criteriaQuery, Root<?> root,
			WhereGroup whereGroup, LogicalOperator parentOperator) {
		if (!whereGroup.getConditions().isEmpty()) {
			Predicate[] predicates = whereGroup.getConditions().stream().map(condition -> {
				if (condition.getConditions() != null) {
					Subquery<?> subQuery = criteriaQuery.subquery(root.getModel().getBindableJavaType());
					Root<?> subRoot = subQuery.from(root.getModel().getBindableJavaType());
					addNestedWhereClause(criteriaBuilder, subQuery, subRoot, condition, whereGroup.getOperator());
					return criteriaBuilder.exists(subQuery);
				} else {
					return createPredicate(criteriaBuilder, root, condition, parentOperator);
				}
			}).toArray(Predicate[]::new);

			Predicate finalPredicate;
			if (parentOperator == LogicalOperator.AND) {
				finalPredicate = criteriaBuilder.and(predicates);
			} else {
				finalPredicate = criteriaBuilder.or(predicates);
			}
			criteriaQuery.where(finalPredicate);
		}
	}

	private Predicate createPredicate(CriteriaBuilder criteriaBuilder, Root<?> root, Condition condition,
			LogicalOperator parentOperator) {
		if (parentOperator == LogicalOperator.AND) {
			return criteriaBuilder.and(criteriaBuilder.conjunction(),
					createConditionPredicate(criteriaBuilder, root, condition));
		} else {
			return criteriaBuilder.or(criteriaBuilder.disjunction(),
					createConditionPredicate(criteriaBuilder, root, condition));
		}
	}

	public TakeSkipParams parseTakeSkipParams(ApiType apiType, ListQueryOptions<?> options, boolean ignoreQueryLimits) {
		int shopListQueryLimit = configService.getApiOptions().getShopListQueryLimit();
		int adminListQueryLimit = configService.getApiOptions().getAdminListQueryLimit();
		int takeLimit = ignoreQueryLimits ? Integer.MAX_VALUE
				: (apiType == ApiType.ADMIN ? adminListQueryLimit : shopListQueryLimit);

		if (options.getTake() != null && options.getTake() > takeLimit) {
			throw new UserInputException("error.list-query-limit-exceeded",
					Collections.singletonMap("limit", takeLimit));
		}

		int skip = Math.max(options.getSkip() != null ? options.getSkip() : 0, 0);
		int take = options.getTake() != null ? Math.min(Math.max(options.getTake(), 0), takeLimit) : takeLimit;
		if (options.getSkip() != null && options.getTake() == null) {
			take = takeLimit;
		}

		return new TakeSkipParams(take, skip);
	}

//	public List<String> getMinimumRequiredRelations(EntityManager repository, ListQueryOptions<?> options,
//			ExtendedListQueryOptions<?> extendedOptions) {
//		List<String> requiredRelations = new ArrayList<>();
//
//		if (extendedOptions.getChannelId() != null) {
//			requiredRelations.add("channels");
//		}
//
//		Map<String, String> customPropertyMap = extendedOptions.getCustomPropertyMap();
//		if (customPropertyMap != null) {
//			Metamodel metadata = repository.getMetamodel();
//			for (Map.Entry<String, String> entry : customPropertyMap.entrySet()) {
//				String property = entry.getKey();
//				String path = entry.getValue();
//				if (!customPropertyIsBeingUsed(property, options)) {
//					continue;
//				}
//				String[] relationPath = path.split("\\.");
//				EntityType<?> targetMetadata = (EntityType<?>) metadata.managedType(repository.getDomainClass());
//				List<String> reconstructedPath = new ArrayList<>();
//				for (String relationPathPart : relationPath) {
//					SingularAttribute<?, ?> attribute = targetMetadata.getSingularAttribute(relationPathPart);
//					if (attribute != null && attribute instanceof SingularAttribute) {
//						reconstructedPath.add(attribute.getName());
//						requiredRelations.add(String.join(".", reconstructedPath));
//						targetMetadata = (EntityType<?>) attribute.getType();
//					}
//				}
//			}
//		}
//
//		return requiredRelations.stream().distinct().collect(Collectors.toList());
//	}
	
	private static <T extends VendureEntity> List<String> getMinimumRequiredRelations(
            CrudRepository<T, ?> repository,
            ListQueryOptions<T> options,
            ExtendedListQueryOptions<T> extendedOptions) {
        List<String> requiredRelations = new ArrayList<>();
        if (extendedOptions.getChannelId() != null) {
            requiredRelations.add("channels");
        }

        if (extendedOptions.getCustomPropertyMap() != null) {
            Map<String, String> customPropertyMap = extendedOptions.getCustomPropertyMap();
            for (Map.Entry<String, String> entry : customPropertyMap.entrySet()) {
                String property = entry.getKey();
                String path = entry.getValue();
                try {
					if (!customPropertyIsBeingUsed(property, options)) {
					    continue;
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                String[] relationPath = path.split("\\.");
                List<String> reconstructedPath = new ArrayList<>();
                EntityMetadata targetMetadata = repository.getEntityMetadata();
                for (String relationPathPart : relationPath) {
                    RelationMetadata relationMetadata = targetMetadata.findRelationWithPropertyPath(relationPathPart);
                    if (relationMetadata != null) {
                        reconstructedPath.add(relationMetadata.getPropertyName());
                        requiredRelations.add(String.join(".", reconstructedPath));
                        targetMetadata = relationMetadata.getInverseEntityMetadata();
                    }
                }
            }
        }
        return requiredRelations.stream().distinct().collect(Collectors.toList());
    }

	

	public boolean customPropertyIsBeingUsed(String property, ListQueryOptions<?> options) {
		return options.getSort() != null && options.getSort().containsKey(property)
				|| options.getFilter() != null && options.getFilter().containsKey(property);
	}

	public void normalizeCustomPropertyMap(Map<String, String> customPropertyMap, ListQueryOptions<?> options,
			SelectQueryBuilder<?> qb) {
		for (Map.Entry<String, String> entry : customPropertyMap.entrySet()) {
			String property = entry.getKey();
			String value = entry.getValue();
			if (!customPropertyIsBeingUsed(property, options)) {
				continue;
			}
			String[] parts = value.split("\\.");
			List<String> normalizedRelationPath = new ArrayList<>();
			EntityType<?> entityMetadata = (EntityType<?>) qb.getExpressionMap().getMainAlias().getMetadata();
			String entityAlias = qb.getAlias();
			while (parts.length > 1) {
				String entityPart = parts.length >= 2 ? parts[0] : qb.getAlias();
				String columnPart = parts[parts.length - 1];

				if (entityMetadata == null) {
					logger.error("Could not get metadata for entity " + qb.getAlias());
					return;
				}
				SingularAttribute<?, ?> attribute = entityMetadata.getSingularAttribute(entityPart);
				if (attribute == null || !(attribute instanceof SingularAttribute)) {
					logger.error("The customPropertyMap entry \"" + property + ":" + value
							+ "\" could not be resolved to a related table");
					customPropertyMap.remove(property);
					return;
				}
				String alias = entityMetadata.getTableName() + "_" + attribute.getName();
				if (!isRelationAlreadyJoined(qb, alias)) {
					qb.leftJoinAndSelect(entityAlias + "." + attribute.getName(), alias);
				}
				parts = Arrays.copyOfRange(parts, 1, parts.length);
				entityMetadata = (EntityType<?>) attribute.getType();
				normalizedRelationPath.add(entityAlias);

				if (parts.length == 1) {
					normalizedRelationPath.add(alias);
					normalizedRelationPath.add(columnPart);
				} else {
					entityAlias = alias;
				}
			}
			customPropertyMap.put(property, String.join(".",
					normalizedRelationPath.subList(normalizedRelationPath.size() - 2, normalizedRelationPath.size())));
		}
	}

	private boolean isRelationAlreadyJoined(SelectQueryBuilder<?> qb, String alias) {
		return qb.getExpressionMap().getJoinAttributes().stream().anyMatch(ja -> ja.getAlias().getName().equals(alias));
	}

	public void joinCalculatedColumnRelations(SelectQueryBuilder<?> qb, Class<?> entity, ListQueryOptions<?> options) {
		List<CalculatedColumn> calculatedColumns = getCalculatedColumns(entity);
		Set<String> filterAndSortFields = new HashSet<>();
		filterAndSortFields.addAll(options.getFilter() != null ? options.getFilter().keySet() : Collections.emptySet());
		filterAndSortFields.addAll(options.getSort() != null ? options.getSort().keySet() : Collections.emptySet());
		String alias = getEntityAlias(entityManager, entity);
		for (String field : filterAndSortFields) {
			CalculatedColumn calculatedColumnDef = calculatedColumns.stream().filter(c -> c.getName().equals(field))
					.findFirst().orElse(null);
			if (calculatedColumnDef != null) {
				List<String> relations = calculatedColumnDef.getListQuery() != null
						? calculatedColumnDef.getListQuery().getRelations()
						: Collections.emptyList();
				for (String relation : relations) {
					boolean relationIsAlreadyJoined = qb.getExpressionMap().getJoinAttributes().stream()
							.anyMatch(ja -> ja.getEntityOrProperty().equals(alias + "." + relation));
					if (!relationIsAlreadyJoined) {
						String propertyPath = relation.contains(".") ? relation : alias + "." + relation;
						String relationAlias = relation.contains(".")
								? relation.split("\\.")[relation.split("\\.").length - 1]
								: relation;
						qb.innerJoinAndSelect(propertyPath, relationAlias);
					}
				}
				if (calculatedColumnDef.getListQuery() != null && calculatedColumnDef.getListQuery().getQuery() != null
						&& calculatedColumnDef.getListQuery().getQuery() instanceof Consumer) {
					((Consumer<SelectQueryBuilder<?>>) calculatedColumnDef.getListQuery().getQuery()).accept(qb);
				}
			}
		}
	}

	public void applyTranslationConditions(SelectQueryBuilder<?> qb, Class<?> entity, Map<String, ?> sortParams,
			RequestContext ctx) {
		String languageCode = ctx != null && ctx.getLanguageCode() != null ? ctx.getLanguageCode()
				: this.configService.getDefaultLanguageCode();

		ColumnMetadata columnMetadata = getColumnMetadata(entityManager, entity);
		String alias = qb.getAlias();

		boolean sortingOnTranslatableKey = false;
		for (ColumnMetadata translationColumn : columnMetadata.getTranslationColumns()) {
			if (sortParams.containsKey(translationColumn.getPropertyName())) {
				sortingOnTranslatableKey = true;
				break;
			}
		}

		if (!columnMetadata.getTranslationColumns().isEmpty() && sortingOnTranslatableKey) {
			String translationsAlias = qb.getConnection().getNamingStrategy().joinTableName(alias, "translations", "",
					"");
			if (!isRelationAlreadyJoined(qb, translationsAlias)) {
				qb.leftJoinAndSelect(alias + ".translations", translationsAlias);
			}

			qb.andWhere(qb1 -> {
				qb1.where(translationsAlias + ".languageCode = :languageCode", languageCode);
				String defaultLanguageCode = ctx != null && ctx.getChannel() != null
						? ctx.getChannel().getDefaultLanguageCode()
						: this.configService.getDefaultLanguageCode();
				Class<?> translationEntity = columnMetadata.getTranslationColumns().get(0).getEntityMetadata()
						.getTarget();

				if (!languageCode.equals(defaultLanguageCode)) {
					qb1.orWhere(qb2 -> {
						String subQb1 = entityManager
								.createQuery("SELECT 1 FROM " + translationEntity.getSimpleName() + " translation "
										+ "WHERE translation.base = " + alias + ".id "
										+ "AND translation.languageCode = :defaultLanguageCode")
								.setParameter("defaultLanguageCode", defaultLanguageCode).setMaxResults(1)
								.getResultList().isEmpty() ? "0" : "1";

						String subQb2 = entityManager
								.createQuery("SELECT 1 FROM " + translationEntity.getSimpleName() + " translation "
										+ "WHERE translation.base = " + alias + ".id "
										+ "AND translation.languageCode = :nonDefaultLanguageCode")
								.setParameter("nonDefaultLanguageCode", languageCode).setMaxResults(1).getResultList()
								.isEmpty() ? "0" : "1";

						qb2.where("EXISTS (" + subQb1 + ")").andWhere("NOT EXISTS (" + subQb2 + ")");
					});
				} else {
					qb1.orWhere(qb2 -> {
						String subQb1 = entityManager
								.createQuery("SELECT 1 FROM " + translationEntity.getSimpleName() + " translation "
										+ "WHERE translation.base = " + alias + ".id "
										+ "AND translation.languageCode = :defaultLanguageCode")
								.setParameter("defaultLanguageCode", defaultLanguageCode).setMaxResults(1)
								.getResultList().isEmpty() ? "0" : "1";

						String subQb2 = entityManager
								.createQuery("SELECT 1 FROM " + translationEntity.getSimpleName() + " translation "
										+ "WHERE translation.base = " + alias + ".id "
										+ "AND translation.languageCode != :defaultLanguageCode")
								.setParameter("defaultLanguageCode", defaultLanguageCode).setMaxResults(1)
								.getResultList().isEmpty() ? "0" : "1";

						qb2.where("NOT EXISTS (" + subQb1 + ")").andWhere("EXISTS (" + subQb2 + ")");
					});
				}
			}).setParameters(
					Map.of("nonDefaultLanguageCode", languageCode, "defaultLanguageCode", defaultLanguageCode));
		}
	}

	public void registerSQLiteRegexpFunction() {
		String dbType = this.connection.getRawConnection().getOptions().getType();
		if (dbType.equals("better-sqlite3")) {
			BetterSqlite3Driver driver = (BetterSqlite3Driver) this.connection.getRawConnection().getDriver();
			driver.getDatabaseConnection().createFunction("regexp", (pattern, value) -> {
				boolean result = Pattern.compile((String) pattern).matcher((String) value).find();
				return result ? 1 : 0;
			});
		}
		if (dbType.equals("sqljs")) {
			SqljsDriver driver = (SqljsDriver) this.connection.getRawConnection().getDriver();
			driver.getDatabaseConnection().createFunction("regexp", (pattern, value) -> {
				boolean result = Pattern.compile((String) pattern).matcher((String) value).find();
				return result ? 1 : 0;
			});
		}
	}

	public boolean isRelationAlreadyJoined(SelectQueryBuilder<?> qb, String alias) {
		return qb.getExpressionMap().getJoinAttributes().stream().anyMatch(ja -> ja.getAlias().getName().equals(alias));
	}

	public Map<String, Integer> parseTakeSkipParams(ApiType apiType, ListQueryOptions<?> options,
			boolean ignoreQueryLimits) {
		Map<String, Integer> result = new HashMap<>();
		int shopListQueryLimit = this.configService.getApiOptions().getShopListQueryLimit();
		int adminListQueryLimit = this.configService.getApiOptions().getAdminListQueryLimit();
		int takeLimit = ignoreQueryLimits ? Integer.MAX_VALUE
				: apiType.equals(ApiType.ADMIN) ? adminListQueryLimit : shopListQueryLimit;
		if (options.getTake() != null && options.getTake() > takeLimit) {
			throw new UserInputError("error.list-query-limit-exceeded", Collections.singletonMap("limit", takeLimit));
		}
		int skip = Math.max(options.getSkip() != null ? options.getSkip() : 0, 0);
		int take = options.getTake() == null ? takeLimit : Math.min(Math.max(options.getTake(), 0), takeLimit);
		if (options.getSkip() != null && options.getTake() == null) {
			take = takeLimit;
		}
		result.put("take", take);
		result.put("skip", skip);
		return result;
	}

	public List<String> getMinimumRequiredRelations(Repository<?> repository, ListQueryOptions<?> options,
			ExtendedListQueryOptions<?> extendedOptions) {
		List<String> requiredRelations = new ArrayList<>();
		if (extendedOptions.getChannelId() != null) {
			requiredRelations.add("channels");
		}

		Map<String, String> customPropertyMap = extendedOptions.getCustomPropertyMap();
		if (customPropertyMap != null) {
			EntityType<?> entityType = entityManager.getMetamodel().entity(repository.getDomainType());
			for (Map.Entry<String, String> entry : customPropertyMap.entrySet()) {
				String property = entry.getKey();
				String path = entry.getValue();
				if (!customPropertyIsBeingUsed(property, options)) {
					continue;
				}
				String[] relationPath = path.split("\\.");
				EntityType<?> targetMetadata = entityType;
				List<String> reconstructedPath = new ArrayList<>();
				for (String relationPathPart : relationPath) {
					Association<?> association = targetMetadata.getAssociation(relationPathPart);
					if (association instanceof SingularAttribute) {
						SingularAttribute<?, ?> singularAttribute = (SingularAttribute<?, ?>) association;
						reconstructedPath.add(singularAttribute.getName());
						requiredRelations.add(String.join(".", reconstructedPath));
						targetMetadata = entityManager.getMetamodel().entity(singularAttribute.getType().getJavaType());
					}
				}
			}
		}
		return new ArrayList<>(new HashSet<>(requiredRelations));
	}

	// Implement other methods as needed

}

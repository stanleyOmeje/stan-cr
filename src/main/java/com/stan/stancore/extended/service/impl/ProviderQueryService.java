package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.vendingcommon.dto.request.ProviderPage;
import com.systemspecs.remita.vending.vendingcommon.dto.request.ProviderSearchCriteria;
import com.systemspecs.remita.vending.vendingcommon.entity.Product;
import com.systemspecs.remita.vending.vendingcommon.entity.Provider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ProviderQueryService {

    private final EntityManager entityManager;
    private final CriteriaBuilder criteriaBuilder;

    public ProviderQueryService(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.criteriaBuilder = entityManager.getCriteriaBuilder();
    }

    public Page<Provider> getAllProviderWithFilter(ProviderPage providerPage, ProviderSearchCriteria searchCriteria) {
        log.info("Inside get all Product with filter");
        CriteriaQuery<Provider> criteriaQuery = criteriaBuilder.createQuery(Provider.class);
        Root<Provider> providerRoot = criteriaQuery.from(Provider.class);
        Predicate predicate = getPredicate(searchCriteria, providerRoot);

        if (Objects.isNull(predicate)) {
            log.info("No predicate found");
        }
        criteriaQuery.where(predicate);
        getSort(providerPage, criteriaQuery, providerRoot);
        TypedQuery<Provider> typedQuery = entityManager.createQuery(criteriaQuery);
        typedQuery.setFirstResult(providerPage.getPageNo() * providerPage.getPageSize());
        typedQuery.setMaxResults(providerPage.getPageSize());
        Pageable pageable = getPageable(providerPage);
        long productCount = getProductCount(predicate);
        return new PageImpl<>(typedQuery.getResultList(), pageable, productCount);
    }

    Predicate getPredicate(ProviderSearchCriteria searchCriteria, Root<Provider> providerRoot) {
        log.info("Inside get Predicate method");
        List<Predicate> predicates = new ArrayList<>();
        if (Objects.nonNull(searchCriteria.getCode())) {
            predicates.add(criteriaBuilder.equal(providerRoot.get("code"), searchCriteria.getCode()));
        }
        if (Objects.nonNull(searchCriteria.getName())) {
            predicates.add(criteriaBuilder.equal(providerRoot.get("name"), searchCriteria.getName()));
        }
        if (Objects.nonNull(searchCriteria.getDescription())) {
            predicates.add(criteriaBuilder.equal(providerRoot.get("description"), searchCriteria.getDescription()));
        }

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }

    public void getSort(ProviderPage providerPage, CriteriaQuery<Provider> criteriaQuery, Root<Provider> providerRoot) {
        log.info("Inside get Sort method");
        if (providerPage.getDirection().equals(Sort.Direction.ASC)) {
            criteriaQuery.orderBy(criteriaBuilder.asc(providerRoot.get(providerPage.getSortBy())));
        } else {
            criteriaQuery.orderBy(criteriaBuilder.desc(providerRoot.get(providerPage.getSortBy())));
        }
    }

    private Pageable getPageable(ProviderPage providerPage) {
        log.info("Inside get Pageable method");
        Sort sort = Sort.by(providerPage.getDirection(), providerPage.getSortBy());
        return PageRequest.of(providerPage.getPageNo(), providerPage.getPageSize(), sort);
    }

    private long getProductCount(Predicate predicate) {
        log.info("Inside get Product count");
        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<Product> rootCount = countQuery.from(Product.class);
        countQuery.select(criteriaBuilder.count(rootCount)).where(predicate);
        return entityManager.createQuery(countQuery).getSingleResult();
    }
}

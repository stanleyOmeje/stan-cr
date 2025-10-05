package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.vendingcommon.dto.request.ProductPage;
import com.systemspecs.remita.vending.vendingcommon.dto.request.ProductSearchCriteria;
import com.systemspecs.remita.vending.vendingcommon.entity.Product;
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
public class ProductQueryService {

    private final EntityManager entityManager;
    private final CriteriaBuilder criteriaBuilder;

    public ProductQueryService(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.criteriaBuilder = entityManager.getCriteriaBuilder();
    }

    public Page<Product> getAllProductWithFilter(ProductPage productPage, ProductSearchCriteria searchCriteria) {
        log.info("Inside get all Product with filter");
        CriteriaQuery<Product> criteriaQuery = criteriaBuilder.createQuery(Product.class);
        Root<Product> productRoot = criteriaQuery.from(Product.class);
        Predicate predicate = getPredicate(searchCriteria, productRoot);

        if (Objects.isNull(predicate)) {
            log.info("No predicate found");
        }
        criteriaQuery.where(predicate);
        getSort(productPage, criteriaQuery, productRoot);
        TypedQuery<Product> typedQuery = entityManager.createQuery(criteriaQuery);
        typedQuery.setFirstResult(productPage.getPageNo() * productPage.getPageSize());
        typedQuery.setMaxResults(productPage.getPageSize());
        Pageable pageable = getPageable(productPage);
        long productCount = getProductCount(predicate);
        return new PageImpl<>(typedQuery.getResultList(), pageable, productCount);
    }

    Predicate getPredicate(ProductSearchCriteria searchCriteria, Root<Product> productRoot) {
        log.info("Inside get Predicate method");
        List<Predicate> predicates = new ArrayList<>();
        if (Objects.nonNull(searchCriteria.getCategoryCode())) {
            predicates.add(criteriaBuilder.equal(productRoot.get("category").get("code"), searchCriteria.getCategoryCode()));
        }
        if (Objects.nonNull(searchCriteria.getCountry())) {
            predicates.add(criteriaBuilder.equal(productRoot.get("country"), searchCriteria.getCountry()));
        }
        if (Objects.nonNull(searchCriteria.getCountryCode())) {
            predicates.add(criteriaBuilder.equal(productRoot.get("countryCode"), searchCriteria.getCountryCode()));
        }
        if (Objects.nonNull(searchCriteria.getCurrencyCode())) {
            predicates.add(criteriaBuilder.equal(productRoot.get("currencyCode"), searchCriteria.getCurrencyCode()));
        }
        if (Objects.nonNull(searchCriteria.getProductType())) {
            predicates.add(criteriaBuilder.equal(productRoot.get("productType"), searchCriteria.getProductType()));
        }
        if (Objects.nonNull(searchCriteria.getProvider())) {
            predicates.add(criteriaBuilder.equal(productRoot.get("provider"), searchCriteria.getProvider()));
        }
        if (Objects.nonNull(searchCriteria.getCode())) {
            predicates.add(criteriaBuilder.equal(productRoot.get("code"), searchCriteria.getCode()));
        }

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }

    public void getSort(ProductPage productPage, CriteriaQuery<Product> criteriaQuery, Root<Product> productRoot) {
        log.info("Inside get Sort method");
        if (productPage.getDirection().equals(Sort.Direction.ASC)) {
            criteriaQuery.orderBy(criteriaBuilder.asc(productRoot.get(productPage.getSortBy())));
        } else {
            criteriaQuery.orderBy(criteriaBuilder.desc(productRoot.get(productPage.getSortBy())));
        }
    }

    private Pageable getPageable(ProductPage productPage) {
        log.info("Inside get Pageable method");
        Sort sort = Sort.by(productPage.getDirection(), productPage.getSortBy());
        return PageRequest.of(productPage.getPageNo(), productPage.getPageSize(), sort);
    }

    private long getProductCount(Predicate predicate) {
        log.info("Inside get Product count");
        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<Product> rootCount = countQuery.from(Product.class);
        countQuery.select(criteriaBuilder.count(rootCount)).where(predicate);
        return entityManager.createQuery(countQuery).getSingleResult();
    }
}

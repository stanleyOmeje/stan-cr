package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.vendingcommon.dto.request.CustomCommissionFilter;
import com.systemspecs.remita.vending.vendingcommon.dto.request.CustomCommissionPage;
import com.systemspecs.remita.vending.vendingcommon.entity.CustomCommission;
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
public class CustomCommissionQueryService {

    private final EntityManager entityManager;
    private final CriteriaBuilder criteriaBuilder;

    public CustomCommissionQueryService(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.criteriaBuilder = entityManager.getCriteriaBuilder();
    }

    public Page<CustomCommission> getAllCommissionWithFilter(
        CustomCommissionPage customCommissionPage,
        CustomCommissionFilter commissionFilter
    ) {
        log.info(">>>>Inside get all commission with filter");
        CriteriaQuery<CustomCommission> criteriaQuery = criteriaBuilder.createQuery(CustomCommission.class);
        Root<CustomCommission> commissionRoot = criteriaQuery.from(CustomCommission.class);
        Predicate predicate = getCommissionPredicate(commissionFilter, commissionRoot);

        if (Objects.isNull(predicate)) {
            log.info("No predicate found");
        }
        criteriaQuery.where(predicate);
        getCommissionSort(customCommissionPage, criteriaQuery, commissionRoot);
        TypedQuery<CustomCommission> typedQuery = entityManager.createQuery(criteriaQuery);
        typedQuery.setFirstResult(customCommissionPage.getPageNo() * customCommissionPage.getPageSize());
        typedQuery.setMaxResults(customCommissionPage.getPageSize());
        Pageable pageable = getPageable(customCommissionPage);
        long productCount = getCommissionCount(predicate);
        return new PageImpl<>(typedQuery.getResultList(), pageable, productCount);
    }

    Predicate getCommissionPredicate(CustomCommissionFilter commissionFilter, Root<CustomCommission> commissionRoot) {
        log.info(">>>>Inside get Commission Predicate method");
        List<Predicate> predicateList = new ArrayList<>();
        if (Objects.nonNull(commissionFilter.getMerchantId())) {
            predicateList.add(criteriaBuilder.equal(commissionRoot.get("merchantId"), commissionFilter.getMerchantId()));
        }
        if (Objects.nonNull(commissionFilter.getProductCode())) {
            predicateList.add(criteriaBuilder.equal(commissionRoot.get("productCode"), commissionFilter.getProductCode()));
        }
        if (Objects.nonNull(commissionFilter.getProcessor())) {
            predicateList.add(criteriaBuilder.equal(commissionRoot.get("processor"), commissionFilter.getProcessor()));
        }
        return criteriaBuilder.and(predicateList.toArray(new Predicate[0]));
    }

    public void getCommissionSort(
        CustomCommissionPage commissionPage,
        CriteriaQuery<CustomCommission> criteriaQuery,
        Root<CustomCommission> commissionRoot
    ) {
        log.info(">>>>Inside get Sort method");
        if (commissionPage.getDirection().equals(Sort.Direction.ASC)) {
            criteriaQuery.orderBy(criteriaBuilder.asc(commissionRoot.get(commissionPage.getSortBy())));
        } else {
            criteriaQuery.orderBy(criteriaBuilder.desc(commissionRoot.get(commissionPage.getSortBy())));
        }
    }

    private Pageable getPageable(CustomCommissionPage commissionPage) {
        log.info(">>>>Inside get Pageable method");
        Sort sort = Sort.by(commissionPage.getDirection(), commissionPage.getSortBy());
        return PageRequest.of(commissionPage.getPageNo(), commissionPage.getPageSize(), sort);
    }

    private long getCommissionCount(Predicate predicate) {
        log.info(">>>>Inside get Commission count");
        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<CustomCommission> rootCount = countQuery.from(CustomCommission.class);
        countQuery.select(criteriaBuilder.count(rootCount)).where(predicate);
        return entityManager.createQuery(countQuery).getSingleResult();
    }
}

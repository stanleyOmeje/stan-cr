package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.vendingcommon.dto.request.BulkTransactionPage;
import com.systemspecs.remita.vending.vendingcommon.dto.request.BulkTransactionSearchCriteria;
import com.systemspecs.remita.vending.vendingcommon.entity.VendingItems;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
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

@Slf4j
@Service
public class BulkTransactionQueryService {

    private final EntityManager entityManager;
    private final CriteriaBuilder criteriaBuilder;

    public BulkTransactionQueryService(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.criteriaBuilder = entityManager.getCriteriaBuilder();
    }

    Page<VendingItems> fetchBulkTransactionWithFilter(BulkTransactionPage transactionPage, BulkTransactionSearchCriteria searchCriteria) {
        log.info("Inside fetch all Transaction with filter");
        CriteriaQuery<VendingItems> criteriaQuery = criteriaBuilder.createQuery(VendingItems.class);
        Root<VendingItems> transactionRoot = criteriaQuery.from(VendingItems.class);
        Predicate predicate = getTransactionPredicate(searchCriteria, transactionRoot);

        if (Objects.isNull(predicate)) {
            log.info("No predicate found");
        }

        criteriaQuery.where(predicate);
        getTransactionSort(transactionPage, criteriaQuery, transactionRoot);
        TypedQuery<VendingItems> typedQuery = entityManager.createQuery(criteriaQuery);
        typedQuery.setFirstResult(transactionPage.getPageNo() * transactionPage.getPageSize());
        typedQuery.setMaxResults(transactionPage.getPageSize());
        Pageable pageable = getPageable(transactionPage);
        long transactionCount = getTransactionCount(predicate);
        return new PageImpl<>(typedQuery.getResultList(), pageable, transactionCount);
    }

    Predicate getTransactionPredicate(BulkTransactionSearchCriteria searchCriteria, Root<VendingItems> transactionRoot) {
        log.info("Inside get Predicate method for transaction");
        List<Predicate> predicateList = new ArrayList<>();

        if (Objects.nonNull(searchCriteria.getProductCode())) {
            predicateList.add(criteriaBuilder.equal(transactionRoot.get("productCode"), searchCriteria.getProductCode()));
        }
        if (Objects.nonNull(searchCriteria.getCategoryCode())) {
            predicateList.add(criteriaBuilder.equal(transactionRoot.get("categoryCode"), searchCriteria.getCategoryCode()));
        }

        if (Objects.nonNull(searchCriteria.getAccountNumber())) {
            predicateList.add(criteriaBuilder.equal(transactionRoot.get("accountNumber"), searchCriteria.getAccountNumber()));
        }
        if (Objects.nonNull(searchCriteria.getPhoneNumber())) {
            predicateList.add(criteriaBuilder.equal(transactionRoot.get("phoneNumber"), searchCriteria.getPhoneNumber()));
        }
        if (Objects.nonNull(searchCriteria.getBulkClientReference())) {
            predicateList.add(criteriaBuilder.equal(transactionRoot.get("bulkClientReference"), searchCriteria.getBulkClientReference()));
        }
        if (Objects.nonNull(searchCriteria.getVendStatus())) {
            TransactionStatus transactionStatus = TransactionStatus.valueOf(searchCriteria.getVendStatus());
            predicateList.add(criteriaBuilder.equal(transactionRoot.get("vendStatus"), transactionStatus));
        }
        return criteriaBuilder.and(predicateList.toArray(new Predicate[0]));
    }

    public void getTransactionSort(
        BulkTransactionPage transactionPage,
        CriteriaQuery<VendingItems> criteriaQuery,
        Root<VendingItems> transactionRoot
    ) {
        log.info("Inside get Sort method");
        if (transactionPage.getDirection().equals(Sort.Direction.ASC)) {
            criteriaQuery.orderBy(criteriaBuilder.asc(transactionRoot.get(transactionPage.getSortBy())));
        } else {
            criteriaQuery.orderBy(criteriaBuilder.desc(transactionRoot.get(transactionPage.getSortBy())));
        }
    }

    private Pageable getPageable(BulkTransactionPage transactionPage) {
        log.info("Inside get Pageable method");
        Sort sort = Sort.by(transactionPage.getDirection(), transactionPage.getSortBy());
        return PageRequest.of(transactionPage.getPageNo(), transactionPage.getPageSize(), sort);
    }

    private long getTransactionCount(Predicate predicate) {
        log.info("Inside get Transaction count");
        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<VendingItems> rootCount = countQuery.from(VendingItems.class);
        countQuery.select(criteriaBuilder.count(rootCount)).where(predicate);
        return entityManager.createQuery(countQuery).getSingleResult();
    }
}

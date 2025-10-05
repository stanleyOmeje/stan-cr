package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.extended.util.DateUtils;
import com.systemspecs.remita.vending.vendingcommon.dto.request.TransactionPage;
import com.systemspecs.remita.vending.vendingcommon.dto.request.TransactionSearchCriteria;
import com.systemspecs.remita.vending.vendingcommon.entity.Transaction;
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
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class TransactionQueryService {

    private final EntityManager entityManager;
    private final CriteriaBuilder criteriaBuilder;

    public TransactionQueryService(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.criteriaBuilder = entityManager.getCriteriaBuilder();
    }

    Page<Transaction> fetchAllTransactionWithFilter(TransactionPage transactionPage, TransactionSearchCriteria searchCriteria) {
        log.info("Inside fetch all Transaction with filter");
        CriteriaQuery<Transaction> criteriaQuery = criteriaBuilder.createQuery(Transaction.class);
        Root<Transaction> transactionRoot = criteriaQuery.from(Transaction.class);
        Predicate predicate = getTransactionPredicate(searchCriteria, transactionRoot);

        if (Objects.isNull(predicate)) {
            log.info("No predicate found");
        }

        criteriaQuery.where(predicate);
        getTransactionSort(transactionPage, criteriaQuery, transactionRoot);
        TypedQuery<Transaction> typedQuery = entityManager.createQuery(criteriaQuery);
        typedQuery.setFirstResult(transactionPage.getPageNo() * transactionPage.getPageSize());
        typedQuery.setMaxResults(transactionPage.getPageSize());
        Pageable pageable = getPageable(transactionPage);
        long transactionCount = getTransactionCount(predicate);
        return new PageImpl<>(typedQuery.getResultList(), pageable, transactionCount);
    }

    Predicate getTransactionPredicate(TransactionSearchCriteria searchCriteria, Root<Transaction> transactionRoot) {
        log.info("Inside get Predicate method for transaction");
        List<Predicate> predicateList = new ArrayList<>();

        if (Objects.nonNull(searchCriteria.getStatus())) {
            predicateList.add(criteriaBuilder.equal(transactionRoot.get("status"), searchCriteria.getStatus()));
        }
        if (Objects.nonNull(searchCriteria.getProductCode())) {
            predicateList.add(criteriaBuilder.equal(transactionRoot.get("productCode"), searchCriteria.getProductCode()));
        }

        if (Objects.nonNull(searchCriteria.getClientReference())) {
            predicateList.add(criteriaBuilder.equal(transactionRoot.get("clientReference"), searchCriteria.getClientReference()));
        }
        Date startDate = Objects.isNull(searchCriteria.getStartDate()) ? null : DateUtils.asDate(searchCriteria.getStartDate());
        if (Objects.nonNull(searchCriteria.getStartDate())) {
            predicateList.add(criteriaBuilder.equal(transactionRoot.get("createdAt"), startDate));
        }
        Date endDate = Objects.isNull(searchCriteria.getEndDate()) ? null : DateUtils.asDate(searchCriteria.getEndDate());
        if (Objects.nonNull(searchCriteria.getEndDate())) {
            predicateList.add(criteriaBuilder.equal(transactionRoot.get("updatedAt"), endDate));
        }
        if (Objects.nonNull(searchCriteria.getInternalReference())) {
            predicateList.add(criteriaBuilder.equal(transactionRoot.get("internalReference"), searchCriteria.getInternalReference()));
        }
        if (Objects.nonNull(searchCriteria.getCategoryCode())) {
            predicateList.add(criteriaBuilder.equal(transactionRoot.get("categoryCode"), searchCriteria.getCategoryCode()));
        }
        if (Objects.nonNull(searchCriteria.getUserId())) {
            predicateList.add(criteriaBuilder.equal(transactionRoot.get("merchantOrgId"), searchCriteria.getUserId()));
        }
        if (Objects.nonNull(searchCriteria.getIpAddress())) {
            predicateList.add(criteriaBuilder.equal(transactionRoot.get("ipAddress"), searchCriteria.getIpAddress()));
        }

        return criteriaBuilder.and(predicateList.toArray(new Predicate[0]));
    }

    public void getTransactionSort(
        TransactionPage transactionPage,
        CriteriaQuery<Transaction> criteriaQuery,
        Root<Transaction> transactionRoot
    ) {
        log.info("Inside get Sort method");
        if (transactionPage.getDirection().equals(Sort.Direction.ASC)) {
            criteriaQuery.orderBy(criteriaBuilder.asc(transactionRoot.get(transactionPage.getSortBy())));
        } else {
            criteriaQuery.orderBy(criteriaBuilder.desc(transactionRoot.get(transactionPage.getSortBy())));
        }
    }

    private Pageable getPageable(TransactionPage transactionPage) {
        log.info("Inside get Pageable method");
        Sort sort = Sort.by(transactionPage.getDirection(), transactionPage.getSortBy());
        return PageRequest.of(transactionPage.getPageNo(), transactionPage.getPageSize(), sort);
    }

    private long getTransactionCount(Predicate predicate) {
        log.info("Inside get Transaction count");
        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<Transaction> rootCount = countQuery.from(Transaction.class);
        countQuery.select(criteriaBuilder.count(rootCount)).where(predicate);
        return entityManager.createQuery(countQuery).getSingleResult();
    }
}

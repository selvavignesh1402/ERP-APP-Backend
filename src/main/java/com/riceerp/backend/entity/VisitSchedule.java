package com.riceerp.backend.entity;

import org.hibernate.annotations.TenantId;
import com.riceerp.backend.enums.VisitStatus;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "visit_schedules")
public class VisitSchedule {

    @TenantId
    @Column(name = "organization_id")
    private Long organizationId;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "beat_plan_id")
    private BeatPlan beatPlan;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "salesperson_id", nullable = false)
    private User salesperson;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "visit_order", nullable = false)
    private int visitOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VisitStatus status = VisitStatus.PENDING;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BeatPlan getBeatPlan() {
        return beatPlan;
    }

    public void setBeatPlan(BeatPlan beatPlan) {
        this.beatPlan = beatPlan;
    }

    public User getSalesperson() {
        return salesperson;
    }

    public void setSalesperson(User salesperson) {
        this.salesperson = salesperson;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public int getVisitOrder() {
        return visitOrder;
    }

    public void setVisitOrder(int visitOrder) {
        this.visitOrder = visitOrder;
    }

    public VisitStatus getStatus() {
        return status;
    }

    public void setStatus(VisitStatus status) {
        this.status = status;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

}

package com.businessos.modules.crm.contact;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.crm.client.Client;
import com.businessos.modules.company.Company;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "client_contacts", indexes = {
    @Index(name = "idx_contact_company", columnList = "company_id"),
    @Index(name = "idx_contact_client", columnList = "client_id"),
    @Index(name = "idx_contact_email", columnList = "email")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClientContact extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 100)
    private String jobTitle;

    @Column(length = 100)
    private String department;

    @Builder.Default
    @Column(nullable = false)
    private boolean primaryContact = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
}

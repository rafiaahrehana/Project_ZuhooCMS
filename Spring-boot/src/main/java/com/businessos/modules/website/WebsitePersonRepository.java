package com.businessos.modules.website;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WebsitePersonRepository extends JpaRepository<WebsitePerson, Long> {
    List<WebsitePerson> findByCompanyIdAndType(Long companyId, PersonType type);
}

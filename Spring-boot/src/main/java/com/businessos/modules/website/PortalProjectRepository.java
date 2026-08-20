package com.businessos.modules.website;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository("portalProjectRepository")
public interface PortalProjectRepository extends JpaRepository<PortalProject, Long> {
    List<PortalProject> findByCompanyId(Long companyId);
}

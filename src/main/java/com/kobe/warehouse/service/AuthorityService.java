package com.kobe.warehouse.service;

import com.kobe.warehouse.service.dto.AuthorityDTO;
import com.kobe.warehouse.service.dto.PrivillegesDTO;
import com.kobe.warehouse.service.dto.PrivillegesWrapperDTO;
import java.util.List;

public interface AuthorityService {
  void save(AuthorityDTO authority);

  void setPrivilleges(AuthorityDTO authority);

  void delete(String name);

  List<AuthorityDTO> fetch(String search);

  AuthorityDTO fetchOne(String name);

  PrivillegesWrapperDTO fetchPrivillegesByRole(String roleName);

  List<PrivillegesDTO> fetchPrivilleges(String search);
}

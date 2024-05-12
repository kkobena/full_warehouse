package com.kobe.warehouse.web.rest.java_client;

import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.UserDTO;
import com.kobe.warehouse.web.rest.proxy.PublicUserResourceProxy;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/java-client")
public class JavaPublicUserResource extends PublicUserResourceProxy {

  public JavaPublicUserResource(UserService userService) {
    super(userService);
  }

  /**
   * {@code GET /users} : get all users with only public information - calling this method is
   * allowed for anyone.
   *
   * @param pageable the pagination information.
   * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body all users.
   */
  @GetMapping("/users")
  public ResponseEntity<List<UserDTO>> getAllPublicUsers(
      @org.springdoc.core.annotations.ParameterObject Pageable pageable) {
    return super.getAllPublicUsers(pageable);
  }

  /**
   * Gets a list of all roles.
   *
   * @return a string list of all roles.
   */
  @GetMapping("/authorities")
  public List<String> getAuthorities() {
    return super.getAuthorities();
  }
}

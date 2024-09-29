package com.kobe.warehouse.web.rest.java_client;

import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.AdminUserDTO;
import com.kobe.warehouse.service.dto.PasswordChangeDTO;
import com.kobe.warehouse.service.errors.InvalidPasswordException;
import com.kobe.warehouse.web.rest.proxy.AccountResourcesProxy;
import com.kobe.warehouse.web.rest.vm.KeyAndPasswordVM;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/java-client")
public class JavaAccountResource extends AccountResourcesProxy {

    public JavaAccountResource(UserRepository userRepository, UserService userService) {
        super(userRepository, userService);
    }

    @GetMapping("/account")
    public AdminUserDTO getAccount() {
        return super.getAccount();
    }

    @PostMapping("/account")
    public void saveAccount(@Valid @RequestBody AdminUserDTO userDTO) {
        super.saveAccount(userDTO);
    }

    /**
     * {@code POST /account/change-password} : changes the current user's password.
     *
     * @param passwordChangeDto current and new password.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the new password is incorrect.
     */
    @PostMapping(path = "/account/change-password")
    public void changePassword(@RequestBody PasswordChangeDTO passwordChangeDto) {
        super.changePassword(passwordChangeDto);
    }

    /**
     * {@code POST /account/reset-password/init} : Send an email to reset the password of the user.
     *
     * @param mail the mail of the user.
     */
    @PostMapping(path = "/account/reset-password/init")
    public void requestPasswordReset(@RequestBody String mail) {
        super.requestPasswordReset(mail);
    }

    /**
     * {@code POST /account/reset-password/finish} : Finish to reset the password of the user.
     *
     * @param keyAndPassword the generated key and the new password.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the password is incorrect.
     * @throws RuntimeException         {@code 500 (Internal Server Error)} if the password could
     *                                  not be reset.
     */
    @PostMapping(path = "/account/reset-password/finish")
    public void finishPasswordReset(@RequestBody KeyAndPasswordVM keyAndPassword) {
        super.finishPasswordReset(keyAndPassword);
    }

    @GetMapping("/account/tot")
    public ResponseEntity<List<AdminUserDTO>> test() {
        return ResponseEntity.ok().body(List.of(super.getAccount()));
    }
}

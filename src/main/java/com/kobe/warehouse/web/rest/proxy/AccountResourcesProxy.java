package com.kobe.warehouse.web.rest.proxy;

import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.AdminUserDTO;
import com.kobe.warehouse.service.dto.PasswordChangeDTO;
import com.kobe.warehouse.service.errors.EmailAlreadyUsedException;
import com.kobe.warehouse.service.errors.InvalidPasswordException;
import com.kobe.warehouse.web.rest.vm.KeyAndPasswordVM;
import com.kobe.warehouse.web.rest.vm.ManagedUserVM;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public class AccountResourcesProxy {

    private final UserRepository userRepository;
    private final UserService userService;

    public AccountResourcesProxy(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    private static boolean isPasswordLengthInvalid(String password) {
        return (
            StringUtils.isEmpty(password) ||
            password.length() < ManagedUserVM.PASSWORD_MIN_LENGTH ||
            password.length() > ManagedUserVM.PASSWORD_MAX_LENGTH
        );
    }

    protected AdminUserDTO getAccount() {
        return userService.getUserConnectedWithAuthorities().orElseThrow(() -> new AccountResourceException("User could not be found"));
    }

    protected void saveAccount(AdminUserDTO userDTO) {
        String userLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new AccountResourceException("Current user login not found"));
        Optional<User> existingUser = org.springframework.util.StringUtils.hasText(userDTO.getEmail())
            ? userRepository.findOneByEmailIgnoreCase(userDTO.getEmail())
            : Optional.empty();
        if (existingUser.isPresent() && (!existingUser.get().getLogin().equalsIgnoreCase(userLogin))) {
            throw new EmailAlreadyUsedException();
        }
        Optional<User> user = userRepository.findOneByLogin(userLogin);
        if (user.isEmpty()) {
            throw new AccountResourceException("User could not be found");
        }
        userService.updateUser(
            userDTO.getFirstName(),
            userDTO.getLastName(),
            userDTO.getEmail(),
            userDTO.getLangKey(),
            userDTO.getImageUrl()
        );
    }

    protected void changePassword(PasswordChangeDTO passwordChangeDto) {
        if (isPasswordLengthInvalid(passwordChangeDto.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        userService.changePassword(passwordChangeDto.getCurrentPassword(), passwordChangeDto.getNewPassword());
    }

    protected void requestPasswordReset(String mail) {
        userService.requestPasswordReset(mail);
    }

    protected void finishPasswordReset(KeyAndPasswordVM keyAndPassword) {
        if (isPasswordLengthInvalid(keyAndPassword.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        Optional<User> user = userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey());

        if (user.isEmpty()) {
            throw new AccountResourceException("No user was found for this reset key");
        }
    }

    private static class AccountResourceException extends RuntimeException {

        private AccountResourceException(String message) {
            super(message);
        }
    }
}

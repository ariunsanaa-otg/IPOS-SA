package com.infopharma.ipos_sa.controller;

import com.infopharma.ipos_sa.dto.CreateUserAccountRequest;
import com.infopharma.ipos_sa.dto.UpdateUserAccountRoleRequest;
import com.infopharma.ipos_sa.entity.UserAccount;
import com.infopharma.ipos_sa.mapper.Mapper;
import com.infopharma.ipos_sa.mapper.impl.UserAccountMapper;
import com.infopharma.ipos_sa.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
public class UserAccountController {

    private Mapper<UserAccount, CreateUserAccountRequest> userAccountMapper;

    private final UserService userService;

    public UserAccountController(Mapper<UserAccount, CreateUserAccountRequest> userAccountMapper, UserService userService) {
        this.userService = userService;
        this.userAccountMapper = userAccountMapper;
    }

    @PostMapping("/api/accounts")
    public ResponseEntity<CreateUserAccountRequest> createAccount(@RequestBody CreateUserAccountRequest account) {


        UserAccount userAccount = userAccountMapper.mapFrom(account);


        UserAccount newAccount = userService.createAccount(userAccount);

        CreateUserAccountRequest newAccountRequest = userAccountMapper.mapTo(newAccount);

        return new ResponseEntity<>(newAccountRequest, HttpStatus.CREATED);

    }

    @PatchMapping("/api/accounts/{id}/role")
    public ResponseEntity<UpdateUserAccountRoleRequest> updateAccountRole(
           @PathVariable("id") Long id, @RequestBody UpdateUserAccountRoleRequest accountRequest) {

        Optional<UserAccount> accountFetched = userService.findOne(id);

        if (accountFetched.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        UserAccount userAccount = accountFetched.get();

        // Manually update only the role field
        userAccount.setAccountType(accountRequest.getAccountType());

        // Save the updated account
        UserAccount updatedAccount = userService.updateAccount(userAccount);

        // Manually create response DTO
        UpdateUserAccountRoleRequest response = new UpdateUserAccountRoleRequest();
        response.setAccountId(updatedAccount.getAccountId());
        response.setAccountType(updatedAccount.getAccountType());

        return new ResponseEntity<>(response, HttpStatus.OK);


    }

    @DeleteMapping("/api/accounts/{id}")
    public ResponseEntity<Void> deleteAccount(
            @PathVariable("id") Long id) {

        Optional<UserAccount> accountFetched = userService.findOne(id);

        if (accountFetched.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Delete account
        userService.deleteAccount(id);

        return ResponseEntity.noContent().build();

    }
}

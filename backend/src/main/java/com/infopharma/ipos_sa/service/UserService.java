package com.infopharma.ipos_sa.service;

import com.infopharma.ipos_sa.entity.UserAccount;

import java.util.Optional;

public interface UserService {

    UserAccount createAccount(UserAccount account);
    UserAccount updateAccount(UserAccount account);
    void deleteAccount(Long id);

    Optional<UserAccount> findOne(Long id);
}

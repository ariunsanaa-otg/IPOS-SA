package com.infopharma.ipos_sa.dto;

import com.infopharma.ipos_sa.entity.UserAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateUserAccountRequest {

    private String username;
    private String password;
    private UserAccount.AccountType accountType;
    private UserAccount.AccountStatus accountStatus;
    private String contactName;
    private String companyName;
    private String address;
    private String phone;
    private String fax;
    private String email;
}

package com.recaring.auth.business.command;

import com.recaring.auth.vo.LocalEmail;
import com.recaring.auth.vo.Password;

public record SignInCommand(LocalEmail email, Password password) {
}

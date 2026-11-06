package org.udaan.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.udaan.identity.api.AuthResponse;

/** Public sign-in endpoints under /api/v1/auth (section 7.2). */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private final AuthService auth;
    private final TokenService tokens;

    AuthController(AuthService auth, TokenService tokens) {
        this.auth = auth;
        this.tokens = tokens;
    }

    record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72, message = "must be 8 to 72 characters") String password,
            @NotBlank @Size(min = 2, max = 80) String displayName) {
    }

    record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    record OtpRequest(@NotBlank String phone) {
    }

    record OtpVerifyRequest(
            @NotBlank String phone,
            @NotBlank @Pattern(regexp = "\\s*\\d{6}\\s*", message = "must be 6 digits") String code,
            @Size(max = 80) String displayName) {
    }

    record RefreshRequest(@NotBlank String refreshToken) {
    }

    @GetMapping("/options")
    AuthService.AuthOptions options() {
        return auth.options();
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return auth.register(request.email(), request.password(), request.displayName());
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request.email(), request.password());
    }

    @PostMapping("/otp/request")
    AuthService.OtpIssued requestOtp(@Valid @RequestBody OtpRequest request) {
        return auth.requestOtp(request.phone());
    }

    @PostMapping("/otp/verify")
    AuthResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return auth.verifyOtp(request.phone(), request.code(), request.displayName());
    }

    @PostMapping("/refresh")
    AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return tokens.rotate(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Valid @RequestBody RefreshRequest request) {
        tokens.revoke(request.refreshToken());
    }
}

package com.aicarrental.api.user;

import com.aicarrental.api.user.request.CreateUserRequest;
import com.aicarrental.api.user.request.UpdateUserRequest;
import com.aicarrental.api.user.response.UserResponse;
import com.aicarrental.application.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
 private final UserService userService;
 @PostMapping
 public ResponseEntity<UserResponse> createUser(
         @Valid @RequestBody CreateUserRequest createUserRequest) {
   UserResponse userResponse = userService.createUser(createUserRequest);
   return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
 }

 @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers() {
     List<UserResponse> responses=userService.getAllUsers();
     return ResponseEntity.ok(responses);

 }
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id
    ) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id
    ) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}

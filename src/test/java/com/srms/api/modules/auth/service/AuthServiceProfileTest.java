package com.srms.api.modules.auth.service;

import com.srms.api.exception.BusinessException;
import com.srms.api.modules.auth.dto.UserDto;
import com.srms.api.modules.auth.entity.AppUser;
import com.srms.api.modules.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceProfileTest {
    private UserRepository userRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        authService = new AuthService(userRepository, null, null, null, null, null);
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void parentWithPlaceholderNameCanCompleteProfile() {
        AppUser parent = user("parent-1", "Parent", AppUser.UserRole.PARENT);
        when(userRepository.findById(parent.getId())).thenReturn(Optional.of(parent));

        UserDto updated = authService.updateOwnProfile(
                parent.getId(), "  Mary Banda  ", null, null, null, "Learner guardian");

        assertEquals("Mary Banda", updated.getName());
        assertEquals("MB", updated.getInitials());
        assertEquals("Learner guardian", updated.getBio());
        verify(userRepository).save(parent);
    }

    @Test
    void placeholderIsNotAcceptedAsCompletedParentName() {
        AppUser parent = user("parent-2", "Unknown", AppUser.UserRole.PARENT);
        when(userRepository.findById(parent.getId())).thenReturn(Optional.of(parent));

        BusinessException error = assertThrows(BusinessException.class, () ->
                authService.updateOwnProfile(parent.getId(), "Guardian", null, null, null, null));

        assertEquals("Enter your full name to continue", error.getMessage());
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void completedOrNonParentAccountsCannotRenameThemselves() {
        AppUser parent = user("parent-3", "Mary Banda", AppUser.UserRole.PARENT);
        when(userRepository.findById(parent.getId())).thenReturn(Optional.of(parent));

        assertThrows(BusinessException.class, () ->
                authService.updateOwnProfile(parent.getId(), "Different Name", null, null, null, null));

        AppUser teacher = user("teacher-1", "Teacher", AppUser.UserRole.TEACHER);
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        assertThrows(BusinessException.class, () ->
                authService.updateOwnProfile(teacher.getId(), "New Teacher Name", null, null, null, null));
    }

    private AppUser user(String id, String name, AppUser.UserRole role) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setName(name);
        user.setRole(role);
        user.setEmail(id + "@example.test");
        user.setInitials("P");
        return user;
    }
}

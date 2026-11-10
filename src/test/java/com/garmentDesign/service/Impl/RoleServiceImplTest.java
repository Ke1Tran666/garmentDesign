package com.garmentDesign.service.Impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.garmentDesign.dto.role.RoleUpsertRequest;
import com.garmentDesign.entity.Permission;
import com.garmentDesign.entity.Role;
import com.garmentDesign.repository.PermissionRepository;
import com.garmentDesign.repository.RoleRepository;
import com.garmentDesign.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PermissionRepository permissionRepository;

	private RoleServiceImpl roleService;

	@BeforeEach
	void setUp() {
		roleService = new RoleServiceImpl(roleRepository, userRepository, permissionRepository);
	}

	@Test
	void createAcceptsReadableVietnameseRoleNameWithDynamicAndCustomScopes() {
		Permission existingOrderPerm = permission(6L, "SERVICE_ORDER_PROCESS", "Xử lý đơn dịch vụ");
		when(roleRepository.existsByNameRoleIgnoreCase("Thiết kế rập")).thenReturn(false);
		when(roleRepository.findMaxIdRole()).thenReturn(3L);
		when(permissionRepository.findById(6L)).thenReturn(Optional.of(existingOrderPerm));
		when(permissionRepository.findByNameIgnoreCase("Kiểm duyệt mẫu rập")).thenReturn(Optional.empty());
		when(permissionRepository.findByCodeIgnoreCase("Kiểm duyệt mẫu rập")).thenReturn(Optional.empty());
		when(permissionRepository.existsByCodeIgnoreCase("KIEM_DUYET_MAU_RAP")).thenReturn(false);
		when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> {
			Permission p = invocation.getArgument(0);
			p.setIdPermission(13L);
			return p;
		});
		when(roleRepository.saveAndFlush(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Role created = roleService.create(new RoleUpsertRequest(null, "  Thiết   kế rập  ",
				" Phụ trách thiết kế rập mẫu may đo ", List.of(6L), List.of("Xử lý đơn dịch vụ", "Kiểm duyệt mẫu rập")));

		assertEquals(4L, created.getIdRole());
		assertEquals("Thiết kế rập", created.getNameRole());
		assertEquals("Phụ trách thiết kế rập mẫu may đo", created.getDescription());
		assertEquals(List.of("Xử lý đơn dịch vụ", "Kiểm duyệt mẫu rập"), created.getScopes());
		assertEquals(2, created.getPermissions().size());
		assertFalse(Boolean.TRUE.equals(created.getSystemRole()));
	}

	@Test
	void findAllPermissionsDelegatesOrderedCatalog() {
		List<Permission> catalog = List.of(permission(1L, "SYSTEM_ADMIN", "Quản trị hệ thống"));
		when(permissionRepository.findAllByOrderByIdPermissionAsc()).thenReturn(catalog);

		assertEquals(catalog, roleService.findAllPermissions());
	}

	@Test
	void updateAllowsRenamingDefaultRoleAndUpdatingPermissionsWhileKeepingSystemFlag() {
		Role adminRole = role(1L, "admin");
		Permission sysAdmin = permission(1L, "SYSTEM_ADMIN", "Quản trị hệ thống");
		Permission roleManage = permission(2L, "ROLE_MANAGE", "Quản lý vai trò");
		when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));
		when(roleRepository.existsByNameRoleIgnoreCaseAndIdRoleNot("Quản trị viên cao cấp", 1L)).thenReturn(false);
		when(permissionRepository.findById(1L)).thenReturn(Optional.of(sysAdmin));
		when(permissionRepository.findByNameIgnoreCase("Quản lý vai trò")).thenReturn(Optional.of(roleManage));
		when(roleRepository.saveAndFlush(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(userRepository.countByRole_IdRole(1L)).thenReturn(2L);
		when(userRepository.countByRole_IdRoleAndDeletedAtIsNull(1L)).thenReturn(2L);

		Role updated = roleService.update(1L, new RoleUpsertRequest(null, "  Quản trị viên   cao cấp ",
				"Mô tả quản trị", List.of(1L), List.of("Quản lý vai trò")));

		assertEquals("Quản trị viên cao cấp", updated.getNameRole());
		assertEquals("Mô tả quản trị", updated.getDescription());
		assertEquals(List.of("Quản trị hệ thống", "Quản lý vai trò"), updated.getScopes());
		assertTrue(Boolean.TRUE.equals(updated.getSystemRole()));
		assertEquals(2L, updated.getUserCount());
		assertEquals(2L, updated.getActiveUserCount());
	}

	@Test
	void updateBlocksCustomRoleFromClaimingReservedSystemName() {
		Role customRole = role(4L, "Thiết kế");
		when(roleRepository.findById(4L)).thenReturn(Optional.of(customRole));

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> roleService.update(4L, new RoleUpsertRequest(null, "admin", null, null, null)));

		assertTrue(exception.getMessage().toLowerCase().contains("admin"));
		verify(roleRepository, never()).saveAndFlush(any());
	}

	@Test
	void updateBlocksDefaultRoleFromClaimingAnotherDefaultSlotName() {
		Role staffRole = role(2L, "STAFF");
		when(roleRepository.findById(2L)).thenReturn(Optional.of(staffRole));

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> roleService.update(2L, new RoleUpsertRequest(null, "USER", null, null, null)));

		assertTrue(exception.getMessage().contains("USER"));
		verify(roleRepository, never()).saveAndFlush(any());
	}

	@Test
	void deleteBlocksDefaultSystemRoleEvenAfterRename() {
		Role renamedAdmin = role(1L, "Quản trị viên");
		when(roleRepository.findById(1L)).thenReturn(Optional.of(renamedAdmin));

		RuntimeException exception = assertThrows(RuntimeException.class, () -> roleService.delete(1L, 3L));

		assertTrue(exception.getMessage().contains("Không thể xóa vai trò mặc định"));
		verify(userRepository, never()).reassignUsersRole(any(), any(), any());
		verify(roleRepository, never()).deleteById(any());
	}

	@Test
	void deleteRequiresValidDifferentReplacementRole() {
		Role designerRole = role(4L, "Thiết kế rập");
		when(roleRepository.findById(4L)).thenReturn(Optional.of(designerRole));

		RuntimeException missingReplacement = assertThrows(RuntimeException.class, () -> roleService.delete(4L, null));
		assertTrue(missingReplacement.getMessage().contains("vai trò thay thế"));

		RuntimeException selfReplacement = assertThrows(RuntimeException.class, () -> roleService.delete(4L, 4L));
		assertTrue(selfReplacement.getMessage().contains("phải khác"));

		verify(userRepository, never()).reassignUsersRole(any(), any(), any());
		verify(roleRepository, never()).deleteById(any());
	}

	@Test
	void deleteReassignsUsersAndRemovesCustomRoleWhenReplacementProvided() {
		Role designerRole = role(4L, "Thiết kế rập");
		Role replacementRole = role(3L, "USER");
		when(roleRepository.findById(4L)).thenReturn(Optional.of(designerRole));
		when(roleRepository.findById(3L)).thenReturn(Optional.of(replacementRole));

		roleService.delete(4L, 3L);

		verify(userRepository).reassignUsersRole(eq(4L), eq(replacementRole), any(LocalDateTime.class));
		verify(roleRepository).deleteById(4L);
		verify(roleRepository).flush();
	}

	private static Role role(Long id, String name) {
		Role role = new Role();
		role.setIdRole(id);
		role.setNameRole(name);
		return role;
	}

	private static Permission permission(Long id, String code, String name) {
		Permission perm = new Permission();
		perm.setIdPermission(id);
		perm.setCode(code);
		perm.setName(name);
		return perm;
	}
}

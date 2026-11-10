package com.garmentDesign.service.Impl;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garmentDesign.dto.role.RoleUpsertRequest;
import com.garmentDesign.entity.Permission;
import com.garmentDesign.entity.Role;
import com.garmentDesign.repository.PermissionRepository;
import com.garmentDesign.repository.RoleRepository;
import com.garmentDesign.repository.UserRepository;
import com.garmentDesign.service.RoleService;

@Service
public class RoleServiceImpl implements RoleService {

	private static final Set<Long> SYSTEM_ROLE_IDS = Set.of(1L, 2L, 3L);
	private static final Set<String> SYSTEM_ROLE_NAMES = Set.of("ADMIN", "STAFF", "USER");
	private static final Map<String, Long> RESERVED_NAME_SLOTS = Map.of("ADMIN", 1L, "STAFF", 2L, "USER", 3L);
	private static final Pattern ROLE_NAME_PATTERN = Pattern
			.compile("^[\\p{L}\\p{N}][\\p{L}\\p{N}\\s_\\-&/().,]{1,49}$");

	private final RoleRepository repository;
	private final UserRepository userRepository;
	private final PermissionRepository permissionRepository;

	public RoleServiceImpl(RoleRepository repository, UserRepository userRepository,
			PermissionRepository permissionRepository) {
		this.repository = repository;
		this.userRepository = userRepository;
		this.permissionRepository = permissionRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Role> findAll() {
		List<Role> roles = repository.findAllByOrderByIdRoleAsc();
		Map<Long, RoleUserCounts> countsByRoleId = loadCountsByRoleId();

		roles.forEach(role -> {
			RoleUserCounts counts = countsByRoleId.getOrDefault(role.getIdRole(), RoleUserCounts.ZERO);
			enrichRole(role, counts.totalUsers(), counts.activeUsers());
		});

		return roles;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Permission> findAllPermissions() {
		return permissionRepository.findAllByOrderByIdPermissionAsc();
	}

	@Override
	@Transactional(readOnly = true)
	public Role findById(Long id) {
		Role role = findEntityById(id);
		long totalUsers = userRepository.countByRole_IdRole(role.getIdRole());
		long activeUsers = userRepository.countByRole_IdRoleAndDeletedAtIsNull(role.getIdRole());

		return enrichRole(role, totalUsers, activeUsers);
	}

	@Override
	@Transactional
	public Role create(RoleUpsertRequest request) {
		String roleName = normalizeAndValidateName(request);
		String upperKey = roleName.toUpperCase(Locale.ROOT);

		if (SYSTEM_ROLE_NAMES.contains(upperKey)) {
			throw new RuntimeException("Không thể tạo vai trò trùng tên mặc định của hệ thống (" + roleName + ")");
		}

		if (repository.existsByNameRoleIgnoreCase(roleName)) {
			throw new RuntimeException("Vai trò \"" + roleName + "\" đã tồn tại");
		}

		String description = normalizeAndValidateDescription(request.description());
		Set<Permission> permissions = resolvePermissions(request, Set.of(), false);
		Long roleId = resolveRoleIdForCreate(request.idRole());

		Role role = new Role();
		role.setIdRole(roleId);
		role.setNameRole(roleName);
		role.setDescription(description);
		role.setPermissions(permissions);

		Role saved = repository.saveAndFlush(role);

		return enrichRole(saved, 0L, 0L);
	}

	@Override
	@Transactional
	public Role update(Long id, RoleUpsertRequest request) {
		Role existing = findEntityById(id);
		String roleName = normalizeAndValidateName(request);
		String upperKey = roleName.toUpperCase(Locale.ROOT);

		Long reservedSlot = RESERVED_NAME_SLOTS.get(upperKey);
		if (reservedSlot != null && !reservedSlot.equals(existing.getIdRole())
				&& !roleName.equalsIgnoreCase(existing.getNameRole())) {
			throw new RuntimeException(
					"Tên vai trò \"" + roleName + "\" chỉ dành cho vai trò mặc định tương ứng của hệ thống");
		}

		if (repository.existsByNameRoleIgnoreCaseAndIdRoleNot(roleName, existing.getIdRole())) {
			throw new RuntimeException("Vai trò \"" + roleName + "\" đã tồn tại");
		}

		existing.setNameRole(roleName);

		if (request.description() != null) {
			existing.setDescription(normalizeAndValidateDescription(request.description()));
		}

		if (request.permissionIds() != null || request.scopes() != null) {
			existing.setPermissions(resolvePermissions(request, existing.getPermissions(), true));
		}

		Role saved = repository.saveAndFlush(existing);
		long totalUsers = userRepository.countByRole_IdRole(saved.getIdRole());
		long activeUsers = userRepository.countByRole_IdRoleAndDeletedAtIsNull(saved.getIdRole());

		return enrichRole(saved, totalUsers, activeUsers);
	}

	@Override
	@Transactional
	public void delete(Long id, Long replacementRoleId) {
		Role existing = findEntityById(id);

		if (isSystemRole(existing)) {
			throw new RuntimeException(
					"Không thể xóa vai trò mặc định của hệ thống (" + existing.getNameRole() + ")");
		}

		if (replacementRoleId == null || replacementRoleId <= 0) {
			throw new RuntimeException("Vui lòng chọn vai trò thay thế trước khi xóa");
		}

		if (existing.getIdRole().equals(replacementRoleId)) {
			throw new RuntimeException("Vai trò thay thế phải khác vai trò cần xóa");
		}

		Role replacementRole = findEntityById(replacementRoleId);

		userRepository.reassignUsersRole(existing.getIdRole(), replacementRole, LocalDateTime.now());
		if (existing.getPermissions() != null) {
			existing.getPermissions().clear();
		}
		repository.deleteById(existing.getIdRole());
		repository.flush();
	}

	private Role findEntityById(Long id) {
		if (id == null || id <= 0) {
			throw new RuntimeException("Mã vai trò không hợp lệ");
		}

		return repository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy vai trò với id: " + id));
	}

	private String normalizeAndValidateName(RoleUpsertRequest request) {
		if (request == null || request.nameRole() == null) {
			throw new RuntimeException("Tên vai trò không được để trống");
		}

		String raw = request.nameRole();
		if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0) {
			throw new RuntimeException("Tên vai trò không được chứa ký tự xuống dòng");
		}

		String normalized = raw.trim().replaceAll("\\s+", " ");
		if (normalized.isBlank()) {
			throw new RuntimeException("Tên vai trò không được để trống");
		}

		if (normalized.length() < 2 || normalized.length() > 50) {
			throw new RuntimeException("Tên vai trò phải có độ dài từ 2 đến 50 ký tự");
		}

		if (!ROLE_NAME_PATTERN.matcher(normalized).matches()) {
			throw new RuntimeException(
					"Tên vai trò phải bắt đầu bằng chữ cái hoặc số và không chứa ký tự đặc biệt không hợp lệ");
		}

		return normalized;
	}

	private String normalizeAndValidateDescription(String rawDescription) {
		if (rawDescription == null) {
			return null;
		}

		String trimmed = rawDescription.trim();
		if (trimmed.isEmpty()) {
			return null;
		}

		if (trimmed.length() > 500) {
			throw new RuntimeException("Mô tả vai trò không được vượt quá 500 ký tự");
		}

		return trimmed;
	}

	private Set<Permission> resolvePermissions(RoleUpsertRequest request, Set<Permission> currentPermissions,
			boolean isUpdate) {
		if (request.permissionIds() == null && request.scopes() == null) {
			return isUpdate && currentPermissions != null ? new LinkedHashSet<>(currentPermissions)
					: new LinkedHashSet<>();
		}

		Set<Permission> resolved = new LinkedHashSet<>();
		Set<String> allocatedCodes = new HashSet<>();

		if (request.permissionIds() != null) {
			for (Long permId : request.permissionIds()) {
				if (permId == null || permId <= 0) {
					throw new RuntimeException("Mã quyền hạn không hợp lệ");
				}
				Permission found = permissionRepository.findById(permId)
						.orElseThrow(() -> new RuntimeException("Không tìm thấy quyền hạn với id: " + permId));
				resolved.add(found);
				if (found.getCode() != null) {
					allocatedCodes.add(found.getCode().toUpperCase(Locale.ROOT));
				}
			}
		}

		if (request.scopes() != null) {
			Map<String, Permission> createdInBatch = new HashMap<>();

			for (String rawScope : request.scopes()) {
				String scopeName = rawScope == null ? "" : rawScope.trim().replaceAll("\\s+", " ");
				if (scopeName.isBlank()) {
					continue;
				}
				if (scopeName.length() > 150) {
					throw new RuntimeException("Tên phạm vi quyền hạn không được vượt quá 150 ký tự");
				}

				boolean alreadySelected = resolved.stream().anyMatch(
						p -> scopeName.equalsIgnoreCase(p.getName()) || scopeName.equalsIgnoreCase(p.getCode()));
				if (alreadySelected) {
					continue;
				}

				String lowerName = scopeName.toLowerCase(Locale.ROOT);
				if (createdInBatch.containsKey(lowerName)) {
					resolved.add(createdInBatch.get(lowerName));
					continue;
				}

				Optional<Permission> existingPerm = permissionRepository.findByNameIgnoreCase(scopeName)
						.or(() -> permissionRepository.findByCodeIgnoreCase(scopeName));

				if (existingPerm.isPresent()) {
					Permission matched = existingPerm.get();
					resolved.add(matched);
					if (matched.getCode() != null) {
						allocatedCodes.add(matched.getCode().toUpperCase(Locale.ROOT));
					}
				} else {
					String generatedCode = generateUniquePermissionCode(scopeName, allocatedCodes);
					Permission created = new Permission();
					created.setCode(generatedCode);
					created.setName(scopeName);

					Permission saved = permissionRepository.save(created);
					Permission finalPermission = saved != null ? saved : created;

					resolved.add(finalPermission);
					allocatedCodes.add(generatedCode);
					createdInBatch.put(lowerName, finalPermission);
				}
			}
		}

		return resolved;
	}

	private String generateUniquePermissionCode(String scopeName, Set<String> allocatedCodes) {
		String normalized = Normalizer.normalize(scopeName, Normalizer.Form.NFD)
				.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").replace('đ', 'd').replace('Đ', 'D')
				.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");

		if (normalized.isBlank()) {
			normalized = "CUSTOM_SCOPE";
		}

		if (normalized.length() > 80) {
			normalized = normalized.substring(0, 80);
		}

		String candidate = normalized;
		int suffix = 2;
		while (allocatedCodes.contains(candidate) || permissionRepository.existsByCodeIgnoreCase(candidate)) {
			candidate = normalized + "_" + suffix;
			suffix++;
		}

		return candidate;
	}

	private Long resolveRoleIdForCreate(Long requestedId) {
		if (requestedId != null) {
			if (requestedId <= 0) {
				throw new RuntimeException("Mã ID vai trò phải là số nguyên dương");
			}

			if (repository.existsById(requestedId)) {
				throw new RuntimeException("Mã ID vai trò đã tồn tại");
			}

			return requestedId;
		}

		Long maxId = repository.findMaxIdRole();

		return Math.max(3L, maxId == null ? 3L : maxId) + 1L;
	}

	private boolean isSystemRole(Role role) {
		if (role == null) {
			return false;
		}

		if (role.getIdRole() != null && SYSTEM_ROLE_IDS.contains(role.getIdRole())) {
			return true;
		}

		String upperName = role.getNameRole() == null ? "" : role.getNameRole().trim().toUpperCase(Locale.ROOT);

		return SYSTEM_ROLE_NAMES.contains(upperName);
	}

	private Role enrichRole(Role role, long totalUsers, long activeUsers) {
		role.setUserCount(totalUsers);
		role.setActiveUserCount(activeUsers);
		role.setSystemRole(isSystemRole(role));

		return role;
	}

	private Map<Long, RoleUserCounts> loadCountsByRoleId() {
		List<Object[]> rows = userRepository.countUsersGroupedByRoleId();
		Map<Long, RoleUserCounts> result = new HashMap<>();

		for (Object[] row : rows) {
			if (row == null || row.length < 3 || row[0] == null) {
				continue;
			}

			Long roleId = ((Number) row[0]).longValue();
			long totalUsers = row[1] == null ? 0L : ((Number) row[1]).longValue();
			long activeUsers = row[2] == null ? 0L : ((Number) row[2]).longValue();

			result.put(roleId, new RoleUserCounts(totalUsers, activeUsers));
		}

		return result;
	}

	private record RoleUserCounts(long totalUsers, long activeUsers) {
		private static final RoleUserCounts ZERO = new RoleUserCounts(0L, 0L);
	}
}

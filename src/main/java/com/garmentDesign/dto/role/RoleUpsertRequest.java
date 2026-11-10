package com.garmentDesign.dto.role;

import java.util.List;

public record RoleUpsertRequest(Long idRole, String nameRole, String description, List<Long> permissionIds,
		List<String> scopes) {
}

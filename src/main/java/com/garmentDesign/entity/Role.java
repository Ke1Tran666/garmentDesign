package com.garmentDesign.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "Roles")
public class Role {
	@Id
	@Column(name = "id_role")
	private Long idRole;

	@Column(name = "name_role", nullable = false, length = 50)
	private String nameRole;

	@Column(name = "description", length = 500)
	private String description;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "Role_Permissions", joinColumns = @JoinColumn(name = "id_role"), inverseJoinColumns = @JoinColumn(name = "id_permission"))
	@OrderBy("idPermission ASC")
	private Set<Permission> permissions = new LinkedHashSet<>();

	@Transient
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Long userCount;

	@Transient
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Long activeUserCount;

	@Transient
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Boolean systemRole;

	public Role() {
	}

	public Long getIdRole() {
		return idRole;
	}

	public void setIdRole(Long idRole) {
		this.idRole = idRole;
	}

	public String getNameRole() {
		return nameRole;
	}

	public void setNameRole(String nameRole) {
		this.nameRole = nameRole;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Set<Permission> getPermissions() {
		return permissions;
	}

	public void setPermissions(Set<Permission> permissions) {
		this.permissions = permissions == null ? new LinkedHashSet<>() : new LinkedHashSet<>(permissions);
	}

	@Transient
	public List<String> getScopes() {
		if (permissions == null || permissions.isEmpty()) {
			return List.of();
		}

		return permissions.stream().map(Permission::getName).filter(name -> name != null && !name.isBlank()).toList();
	}

	public Long getUserCount() {
		return userCount;
	}

	public void setUserCount(Long userCount) {
		this.userCount = userCount;
	}

	public Long getActiveUserCount() {
		return activeUserCount;
	}

	public void setActiveUserCount(Long activeUserCount) {
		this.activeUserCount = activeUserCount;
	}

	public Boolean getSystemRole() {
		return systemRole;
	}

	public void setSystemRole(Boolean systemRole) {
		this.systemRole = systemRole;
	}
}

package com.garmentDesign.entity;

import jakarta.persistence.*;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "Permissions")
public class Permission {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_permission")
	private Long idPermission;

	@Column(name = "code", nullable = false, unique = true, length = 100)
	private String code;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Column(name = "description", length = 300)
	private String description;

	public Permission() {
	}

	public Long getIdPermission() {
		return idPermission;
	}

	public void setIdPermission(Long idPermission) {
		this.idPermission = idPermission;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Permission other)) {
			return false;
		}
		if (this.code != null && other.code != null) {
			return this.code.equalsIgnoreCase(other.code);
		}
		return this.idPermission != null && Objects.equals(this.idPermission, other.idPermission);
	}

	@Override
	public int hashCode() {
		if (code != null) {
			return code.toUpperCase(Locale.ROOT).hashCode();
		}
		return Objects.hashCode(idPermission);
	}
}

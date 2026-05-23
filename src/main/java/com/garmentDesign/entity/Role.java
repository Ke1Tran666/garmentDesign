package com.garmentDesign.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Roles")
public class Role {
    @Id
    @Column(name = "id_role")
    private Long idRole;

    @Column(name = "name_role", nullable = false, length = 50)
    private String nameRole;

    public Role() {}
    public Long getIdRole() { return idRole; }
    public void setIdRole(Long idRole) { this.idRole = idRole; }
    
    public String getNameRole() { return nameRole; }
    public void setNameRole(String nameRole) { this.nameRole = nameRole; }
}

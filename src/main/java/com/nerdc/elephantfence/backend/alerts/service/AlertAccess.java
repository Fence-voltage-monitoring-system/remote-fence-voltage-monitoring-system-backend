package com.nerdc.elephantfence.backend.alerts.service;
import com.nerdc.elephantfence.backend.alerts.entity.Alert;
import com.nerdc.elephantfence.backend.common.security.UserPrincipal;
import com.nerdc.elephantfence.backend.users.entity.*;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import com.nerdc.elephantfence.backend.locations.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.context.SecurityContextHolder;

@Component @RequiredArgsConstructor
public class AlertAccess {
    private final UserRepository users;
    public User currentUser() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Please sign in.");
        return users.findByIdWithProvincesAndDistricts(principal.getId()).filter(User::isEnabled)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Account unavailable."));
    }
    public boolean administer(User u) {return u.getRole()==Role.SUPER_ADMIN || u.getRole()==Role.REGIONAL_ADMIN;}
    public boolean manage(User u) {return administer(u)||u.getRole()==Role.FIELD_ADMIN;}
    public boolean inScope(User u, Long province, Long district) {
        return switch(u.getRole()){
            case SUPER_ADMIN -> true;
            case REGIONAL_ADMIN -> u.getAssignedProvinces().stream().anyMatch(p->p.getId().equals(province));
            case FIELD_ADMIN -> u.getAssignedDistricts().stream().anyMatch(d->d.getId().equals(district));
            default -> false;
        };
    }
    public void requireScope(User u, Alert a){
        if(!(inScope(u,a.getProvinceId(),a.getDistrictId()) ||
            (u.getRole()==Role.MAINTENANCE && u.getId().equals(a.getAssigneeUserId()))))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This incident is outside your authority.");
    }
    public void requireAdmin(User u, Alert a){
        requireScope(u,a);
        if(!administer(u))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only Super and Regional Admins can perform this action.");
    }
    public void requireAssignee(User u, Alert a){
        requireScope(u,a);
        if(!u.getId().equals(a.getAssigneeUserId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only the assigned maintenance user can perform this action.");
    }
    public Specification<Alert> visible(User u){
        return (root,query,cb)->switch(u.getRole()){
            case SUPER_ADMIN -> cb.conjunction();
            case REGIONAL_ADMIN -> u.getAssignedProvinces().isEmpty()?cb.disjunction():root.get("provinceId").in(u.getAssignedProvinces().stream().map(Province::getId).toList());
            case FIELD_ADMIN -> u.getAssignedDistricts().isEmpty()?cb.disjunction():root.get("districtId").in(u.getAssignedDistricts().stream().map(District::getId).toList());
            case MAINTENANCE -> cb.equal(root.get("assigneeUserId"),u.getId());
        };
    }
}


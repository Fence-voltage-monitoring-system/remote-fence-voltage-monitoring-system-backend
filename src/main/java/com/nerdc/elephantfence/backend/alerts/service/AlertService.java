package com.nerdc.elephantfence.backend.alerts.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nerdc.elephantfence.backend.alerts.dto.*;
import com.nerdc.elephantfence.backend.alerts.entity.Alert;
import com.nerdc.elephantfence.backend.alerts.entity.AlertComment;
import com.nerdc.elephantfence.backend.alerts.entity.AlertEvent;
import com.nerdc.elephantfence.backend.alerts.repository.AlertCommentRepository;
import com.nerdc.elephantfence.backend.alerts.repository.AlertEventRepository;
import com.nerdc.elephantfence.backend.alerts.repository.AlertRepository;
import com.nerdc.elephantfence.backend.fences.entity.Fence;
import com.nerdc.elephantfence.backend.fences.repository.FenceRepository;
import com.nerdc.elephantfence.backend.locations.entity.District;
import com.nerdc.elephantfence.backend.locations.entity.Province;
import com.nerdc.elephantfence.backend.locations.repository.DistrictRepository;
import com.nerdc.elephantfence.backend.locations.repository.ProvinceRepository;
import com.nerdc.elephantfence.backend.sections.entity.Section;
import com.nerdc.elephantfence.backend.sections.repository.SectionRepository;
import com.nerdc.elephantfence.backend.users.entity.User;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.support.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertEventRepository alertEventRepository;
    private final AlertCommentRepository alertCommentRepository;
    private final FenceRepository fenceRepository;
    private final SectionRepository sectionRepository;
    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final UserRepository userRepository;
    private final AlertAccess access;
    private final AlertRules rules;
    private final com.nerdc.elephantfence.backend.notifications.service.NotificationService notifications;
    private final AlertWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Transactional(readOnly=true)
    public AlertPageDTO getAlerts(AlertFilters filters,int page,int pageSize){
        if(page<1||pageSize<1||pageSize>100)throw new IllegalArgumentException("Page must be positive; page size must be 1–100.");
        Specification<Alert> spec=access.visible(getCurrentUser());
        if(present(filters.getSeverity()))spec=spec.and(equal("severity",filters.getSeverity()));
        if(present(filters.getStatus()))spec=spec.and(equal("status",filters.getStatus()));
        if(present(filters.getType()))spec=spec.and(equal("type",filters.getType()));
        if(present(filters.getProvince()))spec=spec.and(equal("provinceId",provinceRepository.findByNameIgnoreCase(filters.getProvince()).map(Province::getId).orElse(-1L)));
        if(present(filters.getFence()))spec=spec.and(equal("fenceId",fenceRepository.findByCodeIgnoreCase(filters.getFence()).map(Fence::getId).orElse(-1L)));
        if(present(filters.getDate())){
            OffsetDateTime since=switch(filters.getDate()){case "today"->today();case "7d"->today().minusDays(6);case "30d"->today().minusDays(29);default->throw new IllegalArgumentException("Invalid date filter.");};
            spec=spec.and((r,q,c)->c.greaterThanOrEqualTo(r.get("createdAt"),since));
        }
        Page<Alert> result=alertRepository.findAll(spec,PageRequest.of(page-1,pageSize,Sort.by(Sort.Direction.DESC,"createdAt","id")));
        return AlertPageDTO.builder().items(result.getContent().stream().map(this::toResponse).toList()).page(page).pageSize(pageSize).totalItems(result.getTotalElements()).totalPages(result.getTotalPages()).build();
    }
    private boolean present(String s){return s!=null&&!s.isBlank();}
    private Specification<Alert> equal(String field,Object value){return (r,q,c)->c.equal(r.get(field),value);}
    private OffsetDateTime today(){return java.time.LocalDate.now(java.time.ZoneId.of("Asia/Colombo")).atStartOfDay(java.time.ZoneId.of("Asia/Colombo")).toOffsetDateTime();}
    @Transactional(readOnly=true)
    public AlertStatsDTO getStats(){
        Specification<Alert> scope=access.visible(getCurrentUser());
        Specification<Alert> active=(r,q,c)->c.notEqual(r.get("status"),"RESOLVED");
        return AlertStatsDTO.builder()
            .activeCritical(alertRepository.count(scope.and(active).and(equal("severity","CRITICAL"))))
            .activeWarnings(alertRepository.count(scope.and(active).and(equal("severity","WARNING"))))
            .unacknowledged(alertRepository.count(scope.and(equal("status","UNACKNOWLEDGED"))))
            .underMaintenance(alertRepository.count(scope.and((r,q,c)->r.get("status").in("ASSIGNED","IN_PROGRESS","UNDER_MAINTENANCE"))))
            .resolvedToday(alertRepository.count(scope.and(equal("status","RESOLVED")).and((r,q,c)->c.greaterThanOrEqualTo(r.get("resolvedAt"),today())))).build();
    }
    @Transactional(readOnly=true)
    public AlertResponseDTO getAlert(String key){
        Alert a=key.matches("[0-9]+")?findAlert(Long.valueOf(key)):alertRepository.findByCode(key).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Incident not found."));
        access.requireScope(getCurrentUser(),a);return toResponse(a);
    }
    @Transactional(readOnly=true)
    public Map<String,Object> options(){
        User user=getCurrentUser();
        List<Map<String,Object>> fences=new ArrayList<>();
        for(Fence f:fenceRepository.findAll()){
            if(access.inScope(user,f.getProvince().getId(),f.getDistrict().getId())){
                Map<String,Object> row=new LinkedHashMap<>();
                row.put("id",f.getId());row.put("code",f.getCode());row.put("name",f.getName());row.put("province",f.getProvince().getName());
                fences.add(row);
            }
        }
        return Map.of("fences",fences,"types",List.of("WIRE_BREAK","DEVICE_OFFLINE","LOW_BATTERY","VOLTAGE_DROP","LOW_VOLTAGE","SOLAR_FAILURE","OTHER"));
    }
    @Transactional
    public AlertResponseDTO create(CreateAlertRequestDTO dto){
        User actor=getCurrentUser();
        Fence f=fenceRepository.findById(dto.getFenceId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Fence not found."));
        Alert a=Alert.builder().code("ALT-"+UUID.randomUUID().toString().substring(0,12).toUpperCase()).title(dto.getTitle().trim()).type(dto.getType()).severity(dto.getSeverity())
            .fenceId(f.getId()).provinceId(f.getProvince().getId()).districtId(f.getDistrict().getId()).sectionId(dto.getSectionId())
            .detectedVoltageKv(dto.getDetectedVoltageKv()).thresholdVoltageKv(dto.getThresholdVoltageKv()).build();
        access.requireScope(actor,a);
        if(!access.manage(actor))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only administrators can register incidents.");
        if(dto.getSectionId()!=null){
            Section section=sectionRepository.findById(dto.getSectionId()).orElseThrow(()->new IllegalArgumentException("Section not found."));
            if(!f.getId().equals(section.getFenceId()))throw new IllegalArgumentException("Section must belong to the selected fence.");
        }
        a = alertRepository.saveAndFlush(a);
        createEvent(a.getId(),"ALERT_CREATED",actor.getFullName(),dto.getDescription().trim());
        User primary=f.getPrimaryMaintenanceUser();
        if(primary!=null&&primary.isEnabled()){assign(a,primary,"AUTO_PRIMARY");createEvent(a.getId(),"AUTO_ASSIGNED",actor.getFullName(),"Assigned to "+primary.getFullName());}
        publish(a,"Incident registered: "+a.getTitle());
        return toResponse(a);
    }
    @Transactional(readOnly=true)
    public List<MaintenanceStaffOptionDTO> getEligibleMaintenance(Long id){
        Alert a=findAlert(id);access.requireAdmin(getCurrentUser(),a);
        return candidates(a).stream().map(u->toStaffOption(u,a)).toList();
    }
    private List<User> candidates(Alert a){
        Map<UUID,User> users=new LinkedHashMap<>();
        if(a.getFenceId()!=null)fenceRepository.findById(a.getFenceId()).ifPresent(f->{
            if(f.getPrimaryMaintenanceUser()!=null)users.put(f.getPrimaryMaintenanceUser().getId(),f.getPrimaryMaintenanceUser());
            f.getBackupMaintenanceUsers().forEach(u->users.put(u.getId(),u));
        });
        fenceRepository.findMaintenanceCandidates(a.getProvinceId(),a.getDistrictId()).forEach(u->users.put(u.getId(),u));
        return users.values().stream().filter(User::isEnabled).filter(u->u.getRole()==com.nerdc.elephantfence.backend.users.entity.Role.MAINTENANCE||u.getRole()==com.nerdc.elephantfence.backend.users.entity.Role.FIELD_ADMIN).toList();
    }
    private List<String> actions(User actor,Alert a){
        List<String> result=new ArrayList<>(List.of("COMMENT"));
        if("RESOLVED".equals(a.getStatus()))return result;
        boolean mine=actor.getId().equals(a.getAssigneeUserId());
        boolean prework=!Set.of("IN_PROGRESS","UNDER_MAINTENANCE").contains(a.getStatus());
        if("UNACKNOWLEDGED".equals(a.getStatus()))result.add("ACKNOWLEDGE");
        if(access.administer(actor)){result.add("RESOLVE");if(prework)result.add("ASSIGN");}
        if(prework&&mine&&"AWAITING_ACCEPTANCE".equals(a.getAssignmentStatus())&&(a.getAcceptanceDeadline()==null||a.getAcceptanceDeadline().isAfter(OffsetDateTime.now()))){result.add("ACCEPT");result.add("DECLINE");}
        if(prework&&mine&&"ACCEPTED".equals(a.getAssignmentStatus()))result.add("START");
        if(mine&&"IN_PROGRESS".equals(a.getStatus())&&"ACCEPTED".equals(a.getAssignmentStatus()))result.add("COMPLETE");
        if(prework&&(mine||access.administer(actor))&&Set.of("AWAITING_ACCEPTANCE","DECLINED").contains(a.getAssignmentStatus()))result.add("ESCALATE");
        return result;
    }
    private Alert actionable(Long id,String action){
        Alert a=findAlert(id);User actor=getCurrentUser();access.requireScope(actor,a);
        if(!actions(actor,a).contains(action))throw new ResponseStatusException(HttpStatus.CONFLICT,"This action is not available for your account or the incident's current state. Refresh and try again.");
        return a;
    }
    @Transactional public AlertResponseDTO acknowledge(Long id){Alert a=actionable(id,"ACKNOWLEDGE");a.setStatus("ACKNOWLEDGED");a.setAcknowledgedByUserId(getCurrentUser().getId());a.setAcknowledgedAt(OffsetDateTime.now());return finish(a,"ACKNOWLEDGED","Incident acknowledged.");}
    @Transactional public AlertResponseDTO acceptAssignment(Long id){Alert a=actionable(id,"ACCEPT");a.setAssignmentStatus("ACCEPTED");a.setStatus("ASSIGNED");a.setAcceptanceDeadline(null);if(a.getAcknowledgedAt()==null){a.setAcknowledgedAt(OffsetDateTime.now());a.setAcknowledgedByUserId(getCurrentUser().getId());}return finish(a,"ASSIGNMENT_ACCEPTED","Assignment accepted.");}
    @Transactional public AlertResponseDTO declineAssignment(Long id,DeclineAssignmentRequestDTO dto){Alert a=actionable(id,"DECLINE");a.setAssignmentStatus("DECLINED");a.setAcceptanceDeadline(null);createEvent(id,"ASSIGNMENT_DECLINED",getCurrentUser().getFullName(),dto.getReason());escalate(a,"System");publish(a,"Assignment declined: "+dto.getReason());return toResponse(a);}
    @Transactional public AlertResponseDTO reassignMaintenance(Long id,ReassignAlertRequestDTO dto){
        Alert a=actionable(id,"ASSIGN");
        User staff=candidates(a).stream().filter(u->u.getId().equals(dto.getStaffId())).findFirst().orElseThrow(()->new IllegalArgumentException("Select an active maintenance user responsible for this fence."));
        UUID previous=a.getAssigneeUserId();a.setAttemptedAssignees("");assign(a,staff,"ADMIN_ASSIGNMENT");
        if(previous!=null&&!previous.equals(staff.getId()))notifyUser(a,previous,"You have been removed from this incident assignment.");
        return finish(a,"REASSIGNED","Assigned to "+staff.getFullName()+". Reason: "+dto.getReason());
    }
    private void assign(Alert a,User user,String source){
        a.setAssigneeUserId(user.getId());a.setAssignmentStatus("AWAITING_ACCEPTANCE");a.setAssignmentSource(source);
        a.setAssignedAt(OffsetDateTime.now());a.setAcceptanceDeadline(OffsetDateTime.now().plusMinutes(rules.number("maintenanceAcceptanceTimeoutMinutes",15)));
        a.setAttemptedAssignees((a.getAttemptedAssignees()==null?"":a.getAttemptedAssignees())+user.getId()+",");
    }
    @Transactional public AlertResponseDTO escalateAssignment(Long id){Alert a=actionable(id,"ESCALATE");escalate(a,getCurrentUser().getFullName());publish(a,"Assignment escalated.");return toResponse(a);}
    private void escalate(Alert a,String actor){
        User backup=null;
        if(rules.enabled("escalationEnabled",true)&&a.getFenceId()!=null){
            backup=fenceRepository.findById(a.getFenceId()).stream().flatMap(f->f.getBackupMaintenanceUsers().stream())
                .filter(User::isEnabled).filter(u->!u.getId().equals(a.getAssigneeUserId()))
                .filter(u->a.getAttemptedAssignees()==null||!a.getAttemptedAssignees().contains(u.getId().toString()))
                .sorted(Comparator.comparing(User::getId)).findFirst().orElse(null);
        }
        if(backup!=null){assign(a,backup,"BACKUP_CLAIM");}
        else{a.setAssignmentStatus("ESCALATED");a.setAcceptanceDeadline(null);}
        createEvent(a.getId(),"ESCALATED",actor,backup==null?"Administrator reassignment required.":"Assigned to backup "+backup.getFullName());
    }
    @Transactional public void expireAssignment(Long id){
        Alert a=findAlert(id);
        if("AWAITING_ACCEPTANCE".equals(a.getAssignmentStatus())&&a.getAcceptanceDeadline()!=null&&!a.getAcceptanceDeadline().isAfter(OffsetDateTime.now())&&!"RESOLVED".equals(a.getStatus())){
            escalate(a,"System");publish(a,"Assignment acceptance deadline expired.");
        }
    }
    @Transactional public AlertResponseDTO startWork(Long id){Alert a=actionable(id,"START");a.setStatus("IN_PROGRESS");return finish(a,"WORK_STARTED","Maintenance work started.");}
    @Transactional public AlertResponseDTO completeWork(Long id,CompleteWorkRequestDTO dto){
        Alert a=actionable(id,"COMPLETE");a.setStatus("UNDER_MAINTENANCE");a.setAssignmentStatus("COMPLETED");a.setHealthyReadingsReceived(0);
        a.setResolutionSummary(dto.getSummary());a.setResolutionCause(dto.getCause());a.setResolutionActions(dto.getActions());
        return finish(a,"WORK_COMPLETED",dto.getSummary()+" · Cause: "+dto.getCause()+" · Actions: "+dto.getActions());
    }
    @Transactional public AlertResponseDTO resolveManually(Long id,ResolveManuallyRequestDTO dto){
        Alert a=actionable(id,"RESOLVE");a.setStatus("RESOLVED");a.setResolvedAt(OffsetDateTime.now());a.setResolutionType("MANUAL");a.setResolutionSummary(dto.getReason());a.setAcceptanceDeadline(null);
        if(a.getAssigneeUserId()!=null)a.setAssignmentStatus("COMPLETED");
        return finish(a,"MANUALLY_RESOLVED",dto.getReason());
    }
    @Transactional public AlertResponseDTO addComment(Long id,AddCommentRequestDTO dto){
        Alert a=actionable(id,"COMMENT");User actor=getCurrentUser();
        alertCommentRepository.save(AlertComment.builder().alertId(id).userId(actor.getId()).userName(actor.getFullName()).comment(dto.getComment().trim()).build());
        return finish(a,"COMMENT_ADDED",dto.getComment().trim());
    }
    private AlertResponseDTO finish(Alert a,String type,String detail){createEvent(a.getId(),type,getCurrentUser().getFullName(),detail);publish(a,detail);return toResponse(a);}
    private Alert findAlert(Long id){return alertRepository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Incident not found."));}
    private User getCurrentUser(){return access.currentUser();}
    private void createEvent(Long id,String type,String actor,String detail){alertEventRepository.save(AlertEvent.builder().alertId(id).eventType(type).actorName(actor).details(detail).build());}
    private MaintenanceStaffOptionDTO toStaffOption(User user,Alert a){
        String responsibility="DISTRICT";
        if(a.getFenceId()!=null){
            Fence f=fenceRepository.findById(a.getFenceId()).orElse(null);
            if(f!=null&&f.getPrimaryMaintenanceUser()!=null&&f.getPrimaryMaintenanceUser().getId().equals(user.getId()))responsibility="PRIMARY";
            else if(f!=null&&f.getBackupMaintenanceUsers().stream().anyMatch(u->u.getId().equals(user.getId())))responsibility="BACKUP";
        }
        return MaintenanceStaffOptionDTO.builder().id(user.getId()).name(user.getFullName()).email(user.getEmail()).responsibility(responsibility).available(user.isEnabled()).build();
    }
    private void notifyUser(Alert a,UUID recipient,String message){
        if(!rules.notifications())return;
        notifications.sendNotification(com.nerdc.elephantfence.backend.notifications.entity.UserNotification.builder()
            .userId(recipient).code("NTF-"+UUID.randomUUID()).title(a.getTitle()).message(message).category(a.getSeverity()).fenceId(a.getFenceId()).sectionId(a.getSectionId()).relatedAlertCode(a.getCode()).channels("IN_APP").build());
    }
    private void publish(Alert a,String message){
        alertRepository.saveAndFlush(a);
        Set<UUID> recipients=new HashSet<>();
        if(a.getAssigneeUserId()!=null)recipients.add(a.getAssigneeUserId());
        for(var role:List.of(com.nerdc.elephantfence.backend.users.entity.Role.SUPER_ADMIN,com.nerdc.elephantfence.backend.users.entity.Role.REGIONAL_ADMIN,com.nerdc.elephantfence.backend.users.entity.Role.FIELD_ADMIN))
            for(User u:userRepository.findByRole(role))if(u.isEnabled()&&access.inScope(u,a.getProvinceId(),a.getDistrictId()))recipients.add(u.getId());
        recipients.forEach(id->notifyUser(a,id,message));
        if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){webSocketHandler.broadcast("");}});
        else webSocketHandler.broadcast("");
    }
    private String getEventLabel(String type, String actor) {
        if ("ALERT_CREATED".equals(type)) return "Alert detected";
        if ("NOTIFICATION_SENT".equals(type)) return "Notification delivered";
        if ("ACKNOWLEDGED".equals(type)) return "Alert acknowledged by " + actor;
        if ("AUTO_ASSIGNED".equals(type)) return "Auto-assigned to " + actor;
        if ("ASSIGNMENT_ACCEPTED".equals(type)) return "Incident assignment accepted by " + actor;
        if ("ASSIGNMENT_DECLINED".equals(type)) return "Assignment declined by " + actor;
        if ("REASSIGNED".equals(type)) return "Reassigned by " + actor;
        if ("ESCALATED".equals(type)) return "Escalated by " + actor;
        if ("WORK_STARTED".equals(type)) return "Maintenance work started by " + actor;
        if ("COMMENT_ADDED".equals(type)) return "Investigation comment added by " + actor;
        if ("WORK_COMPLETED".equals(type)) return "Maintenance work completed by " + actor;
        if ("MANUALLY_RESOLVED".equals(type)) return "Alert manually resolved by " + actor;
        return type;
    }

    private AlertResponseDTO toResponse(Alert alert) {
        String provinceName = null;
        if (alert.getProvinceId() != null) {
            provinceName = provinceRepository.findById(alert.getProvinceId()).map(Province::getName).orElse(null);
        }

        String districtName = null;
        if (alert.getDistrictId() != null) {
            districtName = districtRepository.findById(alert.getDistrictId()).map(District::getName).orElse(null);
        }

        String fenceName = null;
        if (alert.getFenceId() != null) {
            fenceName = fenceRepository.findById(alert.getFenceId()).map(Fence::getCode).orElse(null);
        }

        String sectionCode = null;
        if (alert.getSectionId() != null) {
            sectionCode = sectionRepository.findById(alert.getSectionId()).map(Section::getCode).orElse(null);
        }

        String assigneeName = "Unassigned";
        UUID assigneeId = null;
        if (alert.getAssigneeUserId() != null) {
            User user = userRepository.findById(alert.getAssigneeUserId()).orElse(null);
            if (user != null) {
                assigneeName = user.getFullName();
                assigneeId = user.getId();
            }
        }

        String ackByName = null;
        if (alert.getAcknowledgedByUserId() != null) {
            ackByName = userRepository.findById(alert.getAcknowledgedByUserId()).map(User::getFullName).orElse(null);
        }

        List<String> comments = alertCommentRepository.findByAlertIdOrderByCreatedAtAsc(alert.getId()).stream()
                .map(AlertComment::getComment)
                .toList();

        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        List<AlertEventDTO> timeline = alertEventRepository.findByAlertIdOrderByOccurredAtAsc(alert.getId()).stream()
                .map(event -> AlertEventDTO.builder()
                        .id(event.getId())
                        .type(event.getEventType())
                        .label(getEventLabel(event.getEventType(), event.getActorName()))
                        .timestamp(event.getOccurredAt() != null ? event.getOccurredAt().format(formatter) : "")
                        .actor(event.getActorName())
                        .details(event.getDetails())
                        .build())
                .toList();

        String val = (alert.getDetectedVoltageKv() != null) ? alert.getDetectedVoltageKv() + " kV" : "No signal";
        String thresh = (alert.getThresholdVoltageKv() != null) ? alert.getThresholdVoltageKv() + " kV" : "N/A";
        String detectedStr = alert.getCreatedAt() != null ? alert.getCreatedAt().format(formatter) : "";

        List<MaintenanceStaffOptionDTO> eligible = Collections.emptyList();
        if (alert.getProvinceId() != null && alert.getDistrictId() != null) {
            eligible = fenceRepository.findMaintenanceCandidates(alert.getProvinceId(), alert.getDistrictId()).stream()
                    .map(user -> toStaffOption(user, alert))
                    .toList();
        }

        String assignedAtStr = alert.getAssignedAt() != null ? alert.getAssignedAt().toString() : null;
        String deadlineStr = alert.getAcceptanceDeadline() != null ? alert.getAcceptanceDeadline().toString() : null;
        String ackAtStr = alert.getAcknowledgedAt() != null ? alert.getAcknowledgedAt().toString() : null;

        return AlertResponseDTO.builder()
                .id(alert.getId())
                .code(alert.getCode())
                .title(alert.getTitle())
                .type(alert.getType())
                .severity(alert.getSeverity())
                .province(provinceName)
                .district(districtName)
                .fence(fenceName)
                .section(sectionCode)
                .value(val)
                .threshold(thresh)
                .detected(detectedStr)
                .status(alert.getStatus())
                .assignee(assigneeName)
                .assigneeId(assigneeId)
                .assignmentStatus(alert.getAssignmentStatus())
                .assignmentSource(alert.getAssignmentSource())
                .assignedAt(assignedAtStr)
                .acceptanceDeadline(deadlineStr)
                .eligibleMaintenanceStaff(eligible)
                .comments(comments)
                .timeline(timeline)
                .acknowledgedBy(ackByName)
                .acknowledgedAt(ackAtStr)
                .resolutionType(alert.getResolutionType())
                .allowedActions(actions(getCurrentUser(), alert))
                .resolutionSummary(alert.getResolutionSummary()).resolutionCause(alert.getResolutionCause())
                .resolutionActions(alert.getResolutionActions())
                .healthyReadingsReceived(alert.getHealthyReadingsReceived())
                .healthyReadingsRequired(rules.number("healthyReadingsRequired", 2))
                .build();
    }
}


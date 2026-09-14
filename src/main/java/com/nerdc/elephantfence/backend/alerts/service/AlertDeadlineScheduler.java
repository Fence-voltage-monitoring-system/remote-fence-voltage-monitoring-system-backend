package com.nerdc.elephantfence.backend.alerts.service;
import com.nerdc.elephantfence.backend.alerts.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;
@Component @EnableScheduling @RequiredArgsConstructor @Slf4j
public class AlertDeadlineScheduler {
    private final AlertRepository alerts;
    private final AlertService service;
    @Scheduled(fixedDelay=60000,initialDelay=60000)
    public void expireAssignments(){
        for(var a:alerts.findByAssignmentStatusAndAcceptanceDeadlineBefore("AWAITING_ACCEPTANCE",OffsetDateTime.now())){
            try{service.expireAssignment(a.getId());}
            catch(org.springframework.dao.OptimisticLockingFailureException ignored){/* Another actor updated this incident. */}
            catch(Exception e){log.error("Could not escalate incident {}",a.getId(),e);}
        }
    }
}

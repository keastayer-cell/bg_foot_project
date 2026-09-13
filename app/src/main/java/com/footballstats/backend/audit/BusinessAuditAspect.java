package com.footballstats.backend.audit;

import com.footballstats.backend.security.AppUserPrincipal;
import com.footballstats.backend.service.BusinessAuditService;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.*;

@Aspect @Component @Order(100)
public class BusinessAuditAspect {
    private final BusinessAuditService audit;
    private final EntityManager entityManager;
    public BusinessAuditAspect(BusinessAuditService audit,EntityManager entityManager) {this.audit=audit;this.entityManager=entityManager;}
    @Around("@annotation(action)")
    public Object around(ProceedingJoinPoint call,AuditedAction action) throws Throwable {
        if(!TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("Audited actions require a transaction");
        MethodSignature signature=(MethodSignature)call.getSignature();
        Map<String,Object> args=new HashMap<>();String[] names=signature.getParameterNames();Object[] values=call.getArgs();
        for(int i=0;i<names.length;i++) args.put(names[i],values[i]);
        Object value=args.get(action.idParam());
        if(!(value instanceof Long id)) throw new IllegalStateException("Audit entity ID is missing: "+action.idParam());
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        Long actor=null;String name=null;
        if(authentication!=null && authentication.getPrincipal() instanceof AppUserPrincipal principal) {actor=principal.getUserId();name=principal.getName();}
        else if(args.get(action.actorParam()) instanceof Long actorId) actor=actorId;
        else if(args.get(action.actorParam()) instanceof com.footballstats.backend.service.TeamRepTransferService.TransferActor transferActor) actor=transferActor.userId();
        var before=audit.snapshot(action.entity(),id);
        Object result=call.proceed();
        entityManager.flush();
        audit.record(action.entity(),id,action.action(),actor,name,before,audit.snapshot(action.entity(),id));
        return result;
    }
}

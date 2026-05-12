package com.example.socialnetwork.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ServiceTimingAspect {

    @Around("execution(* com.example.socialnetwork.service..*(..)) || execution(* com.example.socialnetwork.controller.GlobalExceptionHandler..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            log.info("Service method {} executed in {} ms", joinPoint.getSignature().toShortString(), durationMs);
        }
    }
}

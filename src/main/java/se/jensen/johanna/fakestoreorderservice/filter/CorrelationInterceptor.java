package se.jensen.johanna.fakestoreorderservice.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class CorrelationInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
    String id = req.getHeader("X-Correlation-ID");
    if (id != null) {
      MDC.put("correlationId", id);
    }
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler,
      Exception ex) {
    MDC.clear();
  }
}

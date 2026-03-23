package com.e101.carryporter.support;

import com.e101.carryporter.domain.admin.controller.AdminController;
import com.e101.carryporter.domain.admin.service.AdminLockerService;
import com.e101.carryporter.domain.admin.service.AdminService;
import com.e101.carryporter.domain.auth.controller.AuthController;
import com.e101.carryporter.domain.auth.service.AuthService;
import com.e101.carryporter.domain.locker.service.LockerService;
import com.e101.carryporter.domain.mission.controller.MissionController;
import com.e101.carryporter.domain.mission.service.MissionService;
import com.e101.carryporter.domain.robot.service.RobotService;
import com.e101.carryporter.domain.sse.controller.SseController;
import com.e101.carryporter.domain.sse.service.SseService;
import com.e101.carryporter.domain.ticket.controller.TicketController;
import com.e101.carryporter.domain.ticket.service.TicketService;
import com.e101.carryporter.domain.user.controller.UserController;
import com.e101.carryporter.domain.user.service.UserService;
import com.e101.carryporter.global.filter.AuthorizationFilter;
import com.e101.carryporter.global.filter.CorsFilter;
import com.e101.carryporter.global.filter.JwtAuthenticationFilter;
import com.e101.carryporter.global.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
        MissionController.class,
        AuthController.class,
        AdminController.class,
        TicketController.class,
        SseController.class,
        UserController.class,
})
@AutoConfigureMockMvc(addFilters = false)
public abstract class WebMvcTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected UserService userService;

    @MockitoBean
    protected MissionService missionService;

    @MockitoBean
    protected AuthService authService;

    @MockitoBean
    protected RobotService robotService;

    @MockitoBean
    protected AdminLockerService adminLockerService;

    @MockitoBean
    protected AdminService adminService;

    @MockitoBean
    protected TicketService ticketService;

    @MockitoBean
    protected SseService sseService;

    @MockitoBean
    protected LockerService lockerService;

    @MockitoBean
    protected JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    protected AuthorizationFilter authorizationFilter;

    @MockitoBean
    protected CorsFilter corsFilter;

    @MockitoBean
    protected JwtUtils jwtUtils;
}

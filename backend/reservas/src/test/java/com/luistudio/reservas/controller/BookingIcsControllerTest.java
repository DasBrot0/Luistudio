package com.luistudio.reservas.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.luistudio.reservas.security.AuthPrincipal;
import com.luistudio.reservas.service.AccessGuard;
import com.luistudio.reservas.service.BookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class BookingIcsControllerTest {

    @Mock private BookingService bookingService;
    @Mock private AccessGuard accessGuard;
    @InjectMocks private BookingController controller;

    @Test
    void downloadsUtf8CalendarWithAttachmentFilename() throws Exception {
        String ics = "BEGIN:VCALENDAR\r\nLOCATION:Pabellón A1\r\nEND:VCALENDAR\r\n";
        when(accessGuard.requireUser()).thenReturn(new AuthPrincipal(10L, "student@example.com", "ESTUDIANTE"));
        when(bookingService.getStudentIcsContent(1L, 10L)).thenReturn(ics);

        MockMvcBuilders.standaloneSetup(controller).build()
            .perform(get("/api/bookings/1/ics"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("text/calendar")))
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("charset=UTF-8")))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"booking-1.ics\""))
            .andExpect(content().string(ics));
    }
}

package com.luistudio.reservas.service.email;

import com.luistudio.reservas.model.ReservationEntity;
import com.luistudio.reservas.model.RoomEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {
    private static final Pattern URL_PATTERN = Pattern.compile("(https?://\\S+)");

    public String securityCode(String subject, String summary, String code) {
        return branded(subject, summary, List.of(), List.of(), code, null);
    }

    public String callToAction(String subject, String summary, String ctaUrl) {
        return branded(subject, summary, List.of(), List.of(), null, ctaUrl);
    }

    public String alert(String subject, String summary) {
        return branded(subject, summary, List.of(), List.of(), null, null);
    }

    public String absenceNotice(ReservationEntity booking) {
        RoomEntity room = booking.getSala();
        List<Detail> details = List.of(
            new Detail("Sala", room.getNombre()),
            new Detail("Campus", room.getCampus()),
            new Detail("Fecha", String.valueOf(booking.getFecha())),
            new Detail("Horario", booking.getHoraInicio() + " - " + booking.getHoraFin())
        );
        return branded(
            "Inasistencia registrada",
            "No registramos tu presencia en la siguiente reserva. Si fue un error, contacta al administrador.",
            details,
            List.of(),
            null,
            null
        );
    }

    public String roomAvailableAlert(String roomName, java.time.LocalDate date, java.time.LocalTime startTime, java.time.LocalTime endTime) {
        List<Detail> details = List.of(
            new Detail("Sala", roomName),
            new Detail("Fecha", String.valueOf(date)),
            new Detail("Horario", startTime + " - " + endTime)
        );
        return branded(
            "Sala disponible: " + roomName,
            "Una sala que estabas siguiendo quedó disponible para el horario que solicitaste.",
            details,
            List.of(),
            null,
            null
        );
    }

    public String accessAlert(String ip, String userAgent, String when) {
        List<Detail> details = List.of(
            new Detail("IP", ip != null ? ip : "Desconocida"),
            new Detail("Dispositivo", userAgent != null ? userAgent : "Desconocido"),
            new Detail("Fecha y hora", when)
        );
        return branded(
            "Acceso inusual detectado",
            "Se detectó un inicio de sesión desde un dispositivo o ubicación que no reconocemos. Si no fuiste tú, cambia tu contraseña de inmediato.",
            details,
            List.of(),
            null,
            null
        );
    }

    public String bookingStatus(ReservationEntity booking, String subject, String action, String extraInfo) {
        RoomEntity room = booking.getSala();
        List<String> participants = extractParticipants(booking.getObservacion());
        String participantsText = participants.isEmpty()
            ? "Sin integrantes registrados"
            : String.join(" | ", participants);

        List<Detail> details = new ArrayList<>();
        details.add(new Detail("Sala", room.getNombre()));
        details.add(new Detail("Campus", room.getCampus()));
        details.add(new Detail("Recinto", room.getVenue()));
        details.add(new Detail("Ubicación", room.getUbicacion()));
        details.add(new Detail("Fecha", String.valueOf(booking.getFecha())));
        details.add(new Detail("Horario", booking.getHoraInicio() + " - " + booking.getHoraFin()));
        details.add(new Detail("Personas", String.valueOf(booking.getCantidadPersonas())));
        details.add(new Detail("Integrantes", participantsText));
        if (extraInfo != null && !extraInfo.isBlank()) {
            details.add(new Detail("Detalle", extraInfo));
        }

        return branded(subject, "Tu reserva fue " + action + " correctamente.", details, List.of(), null, null);
    }

    public String bookingReminder(ReservationEntity booking, String subject, String summary) {
        RoomEntity room = booking.getSala();
        List<Detail> details = List.of(
            new Detail("Sala", room.getNombre()),
            new Detail("Campus", room.getCampus()),
            new Detail("Recinto", room.getVenue()),
            new Detail("Ubicación", room.getUbicacion()),
            new Detail("Fecha", String.valueOf(booking.getFecha())),
            new Detail("Horario", booking.getHoraInicio() + " - " + booking.getHoraFin())
        );
        return branded(subject, summary, details, List.of(), null, null);
    }

    public String branded(
        String subject,
        String summary,
        List<Detail> details,
        List<String> bullets,
        String code,
        String ctaUrl
    ) {
        String safeSubject = escapeHtml(subject == null || subject.isBlank() ? "Notificación Luistudio" : subject);
        String safeSummary = escapeHtml(summary == null || summary.isBlank()
            ? "Tienes una nueva notificación en Luistudio."
            : summary);

        StringBuilder detailsHtml = new StringBuilder();
        if (details != null && !details.isEmpty()) {
            detailsHtml.append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"border-collapse:separate;border-spacing:0 8px;margin-top:10px;\">");
            for (Detail item : details) {
                if (item == null || item.label() == null || item.value() == null || item.value().isBlank()) {
                    continue;
                }
                detailsHtml.append("<tr>")
                    .append("<td style=\"width:170px;padding:8px 10px;background:#eff6ff;border:1px solid #dbeafe;border-radius:8px;color:#1e3a8a;font-weight:700;font-size:13px;vertical-align:top;\">")
                    .append(escapeHtml(item.label()))
                    .append("</td>")
                    .append("<td style=\"padding:8px 10px;background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;color:#334155;font-size:13px;\">")
                    .append(linkify(escapeHtml(item.value())))
                    .append("</td>")
                    .append("</tr>");
            }
            detailsHtml.append("</table>");
        }

        StringBuilder bulletsHtml = new StringBuilder();
        if (bullets != null && !bullets.isEmpty()) {
            bulletsHtml.append("<ul style=\"margin:10px 0 0 18px;padding:0;color:#334155;\">");
            for (String bullet : bullets) {
                if (bullet != null && !bullet.isBlank()) {
                    bulletsHtml.append("<li style=\"margin:4px 0;\">").append(linkify(escapeHtml(bullet))).append("</li>");
                }
            }
            bulletsHtml.append("</ul>");
        }

        String codeHtml = "";
        if (code != null && !code.isBlank()) {
            codeHtml = "<div style=\"margin-top:14px;padding:12px;border:1px dashed #93c5fd;border-radius:10px;background:#eff6ff;text-align:center;\">"
                + "<div style=\"font-size:12px;color:#1e3a8a;margin-bottom:4px;\">Código de verificación</div>"
                + "<div style=\"font-size:28px;letter-spacing:4px;color:#1d4ed8;font-weight:800;\">"
                + escapeHtml(code)
                + "</div></div>";
        }

        String actionHtml = "";
        if (ctaUrl != null && !ctaUrl.isBlank()) {
            String safeUrl = escapeHtml(ctaUrl);
            actionHtml = "<div style=\"margin-top:16px;\">"
                + "<a href=\"" + safeUrl + "\" style=\"display:inline-block;background:#2563eb;color:#ffffff;text-decoration:none;font-weight:700;padding:11px 16px;border-radius:10px;\">Confirmar cambio</a>"
                + "</div>";
        }

        return "<!DOCTYPE html>"
            + "<html lang=\"es\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"></head>"
            + "<body style=\"margin:0;padding:0;background:#f1f5f9;font-family:Segoe UI,Arial,sans-serif;color:#0f172a;\">"
            + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"padding:24px 12px;\">"
            + "<tr><td align=\"center\">"
            + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:620px;background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #dbe6f3;\">"
            + "<tr><td style=\"background:linear-gradient(120deg,#1e3a8a,#2563eb);padding:22px 24px;color:#ffffff;\">"
            + "<h1 style=\"margin:0;font-size:24px;font-weight:800;\">Luistudio</h1>"
            + "<p style=\"margin:6px 0 0;font-size:15px;opacity:.95;\">Actualización de tu cuenta y reservas</p>"
            + "</td></tr>"
            + "<tr><td style=\"padding:22px 24px;\">"
            + "<h2 style=\"margin:0 0 12px;color:#1e3a8a;font-size:20px;\">" + safeSubject + "</h2>"
            + "<div style=\"background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:14px;color:#334155;line-height:1.5;\">"
            + "<p style=\"margin:0;\">" + safeSummary + "</p>"
            + detailsHtml
            + bulletsHtml
            + codeHtml
            + actionHtml
            + "</div>"
            + "<p style=\"margin:16px 0 0;color:#64748b;font-size:12px;\">Este correo fue generado automáticamente por Luistudio.</p>"
            + "</td></tr></table></td></tr></table></body></html>";
    }

    public boolean isHtml(String body) {
        if (body == null) return false;
        String normalized = body.trim().toLowerCase();
        return normalized.startsWith("<!doctype html") || normalized.startsWith("<html");
    }

    private List<String> extractParticipants(String observation) {
        List<String> values = new ArrayList<>();
        if (observation == null || observation.isBlank()) return values;

        String raw = extractAfterLabel(observation, "Participantes:");
        if (raw == null) raw = extractAfterLabel(observation, "Integrantes:");
        if (raw == null) raw = observation.trim();
        if (raw.isBlank()) return values;

        String[] chunks = raw.split("\\|");
        for (String chunk : chunks) {
            String cleaned = chunk == null ? "" : chunk.trim();
            if (!cleaned.isBlank()) values.add(cleaned);
        }
        return values;
    }

    private String extractAfterLabel(String source, String label) {
        String sourceLower = source.toLowerCase();
        String labelLower = label.toLowerCase();
        int marker = sourceLower.indexOf(labelLower);
        if (marker < 0) return null;
        return source.substring(marker + label.length()).trim();
    }

    private String escapeHtml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private String linkify(String text) {
        if (text == null || text.isBlank()) return "";
        Matcher matcher = URL_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String url = matcher.group(1);
            String replacement = "<a href=\"" + url + "\" style=\"color:#2563eb;text-decoration:underline;\">" + url + "</a>";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public record Detail(String label, String value) {
    }
}

package com.nerdc.elephantfence.backend.reports.service;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.stereotype.Component;

@Component
public class ReportRenderer {
    public record Dataset(String title, List<Map<String,Object>> rows) {}
    public byte[] csv(String title, String range, List<String> warnings, List<Dataset> datasets) {
        var out = new StringBuilder("\uFEFF");
        row(out, List.of(title, range));
        warnings.forEach(w -> row(out, List.of(w)));
        for (var data : datasets) {
            row(out, List.of(data.title()));
            if(data.rows().isEmpty()) { row(out, List.of("No records for this selection.")); continue; }
            row(out, data.rows().getFirst().keySet());
            data.rows().forEach(r -> row(out, r.values()));
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }
    private void row(StringBuilder out, Collection<?> cells) {
        out.append(cells.stream().map(v -> {
            String s = v == null ? "" : v.toString();
            String trimmed = s.stripLeading();
            if((!trimmed.isEmpty() && "=+@-".indexOf(trimmed.charAt(0)) >= 0) || s.startsWith("\t") || s.startsWith("\r")) s = "'" + s;
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }).collect(java.util.stream.Collectors.joining(","))).append("\r\n");
    }
    public byte[] pdf(String title, String range, List<String> warnings, List<Dataset> datasets, boolean charts) {
        try (var doc = new PDDocument(); var out = new ByteArrayOutputStream()) {
            var lines = new ArrayList<String>();
            lines.add(title); lines.add(range); lines.add("Generated: " + java.time.OffsetDateTime.now()); lines.add("");
            lines.addAll(warnings);
            if(charts) {
                lines.add("Record count overview (each # represents approximately 5% of the largest dataset)");
                int max = datasets.stream().mapToInt(d -> d.rows().size()).max().orElse(1);
                for(var d: datasets) lines.add(d.title() + ": " + "#".repeat(max == 0 ? 0 : (int)Math.ceil(20.0*d.rows().size()/max)) + " ("+d.rows().size()+")");
            }
            for(var d: datasets) {
                lines.add(""); lines.add(d.title() + " - " + d.rows().size() + " records");
                if(d.rows().isEmpty()) lines.add("No records for this selection.");
                for(var r:d.rows()) {
                    r.forEach((k,v) -> lines.add(k + ": " + (v == null ? "Not recorded" : v)));
                    lines.add("");
                }
            }
            var wrapped = new ArrayList<String>();
            for(String line:lines) {
                // Standard PDF font supports Latin text; never fail a download on an unsupported character.
                line = line.replaceAll("[^\\x20-\\x7E]", "?");
                if(line.isEmpty()) wrapped.add("");
                for(int i=0;i<line.length();i+=100) wrapped.add(line.substring(i,Math.min(i+100,line.length())));
            }
            for(int offset=0;offset<wrapped.size();offset+=52) {
                var page = new PDPage(); doc.addPage(page);
                try(var content = new PDPageContentStream(doc,page)) {
                    content.beginText(); content.setFont(new PDType1Font(Standard14Fonts.FontName.COURIER),9);
                    content.setLeading(13); content.newLineAtOffset(35,750);
                    for(int i=offset;i<Math.min(offset+52,wrapped.size());i++){content.showText(wrapped.get(i));content.newLine();}
                    content.endText();
                }
            }
            doc.save(out); return out.toByteArray();
        } catch(IOException e) { throw new IllegalStateException("Unable to render report",e); }
    }
}

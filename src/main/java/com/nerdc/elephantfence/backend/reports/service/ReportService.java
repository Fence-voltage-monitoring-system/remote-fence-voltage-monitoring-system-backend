package com.nerdc.elephantfence.backend.reports.service;

import com.nerdc.elephantfence.backend.alerts.service.AlertAccess;
import com.nerdc.elephantfence.backend.users.entity.*;
import com.nerdc.elephantfence.backend.reports.dto.ReportRequest;
import com.nerdc.elephantfence.backend.reports.dto.ReportRequest.*;
import com.nerdc.elephantfence.backend.reports.service.ReportRenderer.Dataset;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ReportService {
    private final NamedParameterJdbcTemplate db;
    private final AlertAccess access;
    private final ReportRenderer renderer;
    private static final ZoneId ZONE = ZoneId.of("Asia/Colombo");
    private record Fence(long id, String code, String name, String province, String district) {}
    private record Prepared(List<Fence> fences, String title, String range, List<String> warnings, List<Dataset> data) {}
    private List<Fence> allowed(User user) {
        return db.query("SELECT f.*, p.name AS province_name, d.name AS district_name FROM fences f JOIN provinces p ON p.id=f.province_id JOIN districts d ON d.id=f.district_id ORDER BY f.code", Map.of(), (rs,n) -> {
            boolean visible = access.inScope(user,rs.getLong("province_id"),rs.getLong("district_id"));
            if(user.getRole()==Role.MAINTENANCE) visible = user.getId().equals(rs.getObject("primary_maintenance_user_id",UUID.class)) ||
                Boolean.TRUE.equals(db.queryForObject("SELECT COUNT(*)>0 FROM fence_backup_maintenance_users WHERE fence_id=:id AND user_id=:user",
                    Map.of("id",rs.getLong("id"),"user",user.getId()),Boolean.class));
            return visible ? new Fence(rs.getLong("id"),rs.getString("code"),rs.getString("name"),rs.getString("province_name"),rs.getString("district_name")) : null;
        }).stream().filter(Objects::nonNull).toList();
    }
    private boolean matches(String selected, String value) { return selected==null || selected.isBlank() || selected.equals(value); }
    private List<Fence> selected(List<Fence> all, Scope scope) {
        var result = all.stream().filter(f -> matches(scope.province(),f.province()) && matches(scope.district(),f.district()) && matches(scope.fence(),f.code())).toList();
        if(result.isEmpty() && (present(scope.province()) || present(scope.district()) || present(scope.fence())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"The selected scope is unavailable or outside your authority.");
        if(present(scope.section())) {
            if(!present(scope.fence()) || result.size()!=1) throw bad("Select a fence before selecting a section.");
            Integer count=db.queryForObject("SELECT COUNT(*) FROM sections WHERE fence_id=:id AND code=:code",Map.of("id",result.getFirst().id(),"code",scope.section()),Integer.class);
            if(count==null || count==0) throw bad("This section does not belong to the selected fence.");
        }
        return result;
    }
    private boolean present(String s) { return s!=null && !s.isBlank(); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
    private List<Map<String,String>> options(List<String> values) {return values.stream().distinct().sorted().map(v->Map.of("value",v,"label",v)).toList();}
    public Map<String,Object> filters(Scope scope) {
        var all=allowed(access.currentUser());
        var chosen=selected(all,scope);
        var districts=all.stream().filter(f->matches(scope.province(),f.province())).map(Fence::district).toList();
        var fences=all.stream().filter(f->matches(scope.province(),f.province())&&matches(scope.district(),f.district())).map(f->Map.of("value",f.code(),"label",f.code()+" · "+f.name())).toList();
        List<Map<String,String>> sections=List.of();
        if(present(scope.fence())&&!chosen.isEmpty()) sections=db.query("SELECT code FROM sections WHERE fence_id=:id ORDER BY code",Map.of("id",chosen.getFirst().id()),(rs,n)->Map.of("value",rs.getString(1),"label",rs.getString(1)));
        return Map.of("provinces",options(all.stream().map(Fence::province).toList()),"districts",options(districts),"fences",fences,"sections",sections);
    }
    private LocalDate[] dates(DateRange range) {
        LocalDate today=LocalDate.now(ZONE), from, to;
        try {
            if(range.preset()==null || range.preset().equals("CUSTOM")) {from=LocalDate.parse(range.from());to=LocalDate.parse(range.to());}
            else {int days=switch(range.preset()){case "TODAY"->1;case "LAST_7_DAYS"->7;case "LAST_30_DAYS"->30;case "LAST_90_DAYS"->90;default->throw bad("Unknown date range.");};to=today;from=to.minusDays(days-1);}
        } catch(java.time.format.DateTimeParseException | NullPointerException e){throw bad("Provide valid start and end dates.");}
        if(from.isAfter(to)||to.isAfter(today)||ChronoUnit.DAYS.between(from,to)>365)throw bad("Choose a date range of at most 366 days ending today or earlier.");
        return new LocalDate[]{from,to};
    }
    private Prepared prepare(ReportRequest r) {
        User user=access.currentUser();
        var fences=selected(allowed(user),r.scope());
        var dates=dates(r.dateRange());
        var p=new MapSqlParameterSource("ids",fences.isEmpty()?List.of(-1L):fences.stream().map(Fence::id).toList())
            .addValue("from",dates[0].atStartOfDay(ZONE).toOffsetDateTime()).addValue("to",dates[1].plusDays(1).atStartOfDay(ZONE).toOffsetDateTime())
            .addValue("section",r.scope().section()).addValue("user",user.getId());
        String section=present(r.scope().section())?" AND s.code=:section":"";
        String alertSection=present(r.scope().section())?" AND a.section_id IN (SELECT id FROM sections WHERE code=:section AND fence_id IN (:ids))":"";
        // Maintenance users may only export incidents assigned to themselves.
        String alertScope=" a.fence_id IN (:ids)"+alertSection+(user.getRole()==Role.MAINTENANCE?" AND a.assignee_user_id=:user":"");
        String alerts="SELECT a.code,a.title,a.type,a.severity,a.status,a.assignment_status,a.created_at,a.acknowledged_at,a.resolved_at FROM alerts a WHERE"+alertScope+" AND a.created_at>=:from AND a.created_at<:to ORDER BY a.created_at";
        String maintenance="SELECT a.code,e.event_type,e.actor_name,e.details,e.occurred_at FROM alert_events e JOIN alerts a ON a.id=e.alert_id WHERE"+alertScope+" AND e.occurred_at>=:from AND e.occurred_at<:to ORDER BY e.occurred_at";
        String sql=switch(r.template()) {
            case FENCE_HEALTH -> present(r.scope().section()) ?
                "SELECT f.code AS fence,s.code AS section,s.status,s.length_km,s.voltage_kv,s.battery,s.updated_at FROM sections s JOIN fences f ON f.id=s.fence_id WHERE f.id IN (:ids)"+section+" ORDER BY s.code" :
                "SELECT f.code,f.name,f.health,f.length_km,f.average_voltage_kv,f.updated_at FROM fences f WHERE f.id IN (:ids) ORDER BY f.code";
            case VOLTAGE_PERFORMANCE -> "SELECT f.code AS fence,s.code AS section,d.name AS device,t.voltage_kv,t.battery,t.signal,t.recorded_at FROM telemetry_readings t JOIN devices d ON d.id=t.device_id LEFT JOIN sections s ON s.id=d.section_id JOIN fences f ON f.id=COALESCE(s.fence_id,d.fence_id) WHERE f.id IN (:ids)"+section+" AND t.recorded_at>=:from AND t.recorded_at<:to ORDER BY t.recorded_at";
            case ALERT_SUMMARY -> alerts;
            case DEVICE_STATUS -> "SELECT f.code AS fence,s.code AS section,d.name,d.serial,d.status,d.voltage,d.battery,d.signal,d.enabled,d.last_seen FROM devices d LEFT JOIN sections s ON s.id=d.section_id JOIN fences f ON f.id=COALESCE(s.fence_id,d.fence_id) WHERE f.id IN (:ids)"+section+" ORDER BY d.id";
            case GATEWAY_CONNECTIVITY -> "SELECT DISTINCT g.name,g.serial,g.status,g.signal,g.power,g.firmware,g.enabled,g.last_seen FROM gateways g JOIN gateway_fences gf ON gf.gateway_id=g.id WHERE gf.fence_id IN (:ids)"+(present(r.scope().section())?" AND g.id IN (SELECT d.gateway_id FROM devices d JOIN sections s ON s.id=d.section_id WHERE s.fence_id IN (:ids) AND s.code=:section)":"")+" ORDER BY g.name";
            case MAINTENANCE -> maintenance;
        };
        String title=r.template().name().replace('_',' ')+" Report";
        var datasets=new ArrayList<Dataset>();datasets.add(dataset(title,sql,p));
        if(r.options().includeAlertHistory()&&r.template()!=Template.ALERT_SUMMARY) datasets.add(dataset("Alert history",alerts,p));
        if(r.options().includeMaintenanceRecords()&&r.template()!=Template.MAINTENANCE) datasets.add(dataset("Incident workflow records",maintenance,p));
        if(datasets.stream().mapToInt(d->d.rows().size()).sum()>5000)throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE,"Select a smaller scope or date range (maximum 5000 rows).");
        var warnings=new ArrayList<String>();
        if(Set.of(Template.FENCE_HEALTH,Template.DEVICE_STATUS,Template.GATEWAY_CONNECTIVITY).contains(r.template()))warnings.add("Status values are current snapshots, not historical uptime. Dates filter only appended history.");
        if(r.template()==Template.MAINTENANCE || r.options().includeMaintenanceRecords())warnings.add("Maintenance records show incident workflow events; planned maintenance schedules are not available.");
        if(r.format()==Format.PDF)warnings.add("PDF uses Latin text. Use CSV to preserve Sinhala or Tamil characters.");
        if(datasets.stream().allMatch(d->d.rows().isEmpty()))warnings.add("No records match this selection.");
        return new Prepared(fences,title,dates[0]+" to "+dates[1]+" (Asia/Colombo)",warnings,datasets);
    }
    private Dataset dataset(String title,String sql,MapSqlParameterSource params) {return new Dataset(title,db.queryForList(sql+" LIMIT 5001",params));}
    public Map<String,Object> preview(ReportRequest request) {
        var p=prepare(request);
        return Map.of("title",p.title(),"recordCount",p.data().stream().mapToInt(d->d.rows().size()).sum(),"scopeLabel",p.fences().size()+" permitted fences","dateRangeLabel",p.range(),"warnings",p.warnings());
    }
    @Transactional
    public Map<String,Object> generate(ReportRequest request) {
        var p=prepare(request);var user=access.currentUser();
        byte[] file=request.format()==Format.CSV?renderer.csv(p.title(),p.range(),p.warnings(),p.data()):renderer.pdf(p.title(),p.range(),p.warnings(),p.data(),request.options().includeCharts());
        var args=new MapSqlParameterSource("name",p.title()).addValue("template",request.template().name()).addValue("user",user.getId()).addValue("range",p.range()).addValue("size",String.format(Locale.ROOT,"%.1f KB",file.length/1024.0)).addValue("format",request.format().name()).addValue("content",file)
            .addValue("ids",p.fences().stream().map(f->Long.toString(f.id())).collect(java.util.stream.Collectors.joining(",")));
        var key=new GeneratedKeyHolder();
        db.update("INSERT INTO generated_reports(name,template_id,generated_by_user_id,date_range_label,status,file_size,format,file_content,fence_ids) VALUES(:name,:template,:user,:range,'READY',:size,:format,:content,:ids)",args,key,new String[]{"id"});
        return historyRow(Objects.requireNonNull(key.getKey()).longValue(),user);
    }
    private final String historySql="SELECT r.id,r.name,u.full_name AS generated_by,r.date_range_label,r.generated_at,r.status,r.file_size,r.format FROM generated_reports r LEFT JOIN users u ON u.id=r.generated_by_user_id";
    private Map<String,Object> mapHistory(java.sql.ResultSet rs) throws java.sql.SQLException {
        var m=new LinkedHashMap<String,Object>();m.put("id",rs.getLong("id"));m.put("name",rs.getString("name"));m.put("generatedBy",rs.getString("generated_by"));m.put("dateRange",rs.getString("date_range_label"));m.put("generatedAt",rs.getObject("generated_at",OffsetDateTime.class).toString());m.put("status",rs.getString("status"));m.put("size",rs.getString("file_size"));m.put("format",rs.getString("format"));return m;
    }
    private Map<String,Object> historyRow(long id,User user) {return db.queryForObject(historySql+" WHERE r.id=:id AND r.generated_by_user_id=:user",Map.of("id",id,"user",user.getId()),(rs,n)->mapHistory(rs));}
    public Map<String,Object> history(int page) {
        if(page<0)throw bad("Invalid page.");
        var user=access.currentUser();var params=Map.of("user",user.getId(),"offset",(long)page*25);
        var items=db.query(historySql+" WHERE r.generated_by_user_id=:user AND r.file_content IS NOT NULL ORDER BY r.generated_at DESC,r.id DESC LIMIT 25 OFFSET :offset",params,(rs,n)->mapHistory(rs));
        var total=db.queryForObject("SELECT COUNT(*) FROM generated_reports WHERE generated_by_user_id=:user AND file_content IS NOT NULL",params,Long.class);
        return Map.of("items",items,"page",page,"pageSize",25,"total",total);
    }
    public record Download(byte[] content,String format) {}
    public Download download(long id) {
        var user=access.currentUser();
        var rows=db.queryForList("SELECT file_content,format,fence_ids FROM generated_reports WHERE id=:id AND generated_by_user_id=:user AND status='READY' AND file_content IS NOT NULL",Map.of("id",id,"user",user.getId()));
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Report unavailable.");
        var row=rows.getFirst();var ids=allowed(user).stream().map(f->Long.toString(f.id())).collect(java.util.stream.Collectors.toSet());
        String stored=(String)row.get("fence_ids");
        if(!stored.isEmpty()&&!ids.containsAll(Arrays.asList(stored.split(","))))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Your access to this report's locations has changed.");
        return new Download((byte[])row.get("file_content"),(String)row.get("format"));
    }
}

package com.kol.kol.api;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import net.bytebuddy.asm.Advice;
import org.springframework.http.codec.ServerSentEvent;
import javax.management.RuntimeErrorException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kol.kol.model.AppUser;
import com.kol.kol.model.RequestProfile;
import com.kol.kol.model.Role;
import com.kol.kol.service.AppUserService;
import com.kol.kol.service.EmailSender;


import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import static java.util.Arrays.stream;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping(path="/api/v1")
public class AppUserController {


    private final AppUserService appUserService;
    private final EmailSender emailSender;



    private List<String> request_profile = new ArrayList<>();


    private String secret_key="2G/pe/o+APbIKXtZHBHem/15fDvr9rLT+5dqvKh/Qz4=";
    @Value("${constants.url}")
    //"http://localhost:8080/
    private String link;

//    /api/v1/approve?token=

    @GetMapping(path = "/users")
    public ResponseEntity<List<AppUser>>getUsers(){
        return ResponseEntity.ok().body(
            appUserService.getAppUsers()
        );
    }
    // @GetMapping(path="/approve",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    // public Flux<?> ApproveRequestProfile(@RequestParam("token") String token){
    //    int n= appUserService.updateApprovedAtToken(token);
    //    log.info("int->{} approved and stored in the DB",n);
    //    RequestProfile requestProfile = appUserService.getRequestProfileProvidedToken(token);
    //    return Flux.interval(Duration.ofSeconds(1))
    //                 .map(e-> requestProfile+" from sse");
    // }
    @GetMapping(path="/approve")
    public String ApproveRequestProfile(@RequestParam("token") String token){
       RequestProfile profile = appUserService.getRequestProfiletoken(token);
        String str = profile.getCreatedAt();
        LocalDateTime createdAt = LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME);
        LocalDateTime time_cal = createdAt.plusHours(12);
        LocalDateTime.ofInstant(Instant.parse(str), ZoneId.systemDefault());
        log.info("time cal {}",time_cal);
        log.info("created at {}",createdAt);
        log.info("time now {}",LocalDateTime.now().minusHours(5).minusMinutes(30));
        if(time_cal.isBefore(LocalDateTime.now().minusHours(5).minusMinutes(30))){
            log.info("link expired");
            appUserService.deleteRequestProfileByKOLID(profile.getKolProfileId());
            return "<h3>Link Expired</h3>";
        }else {
            log.info("Link Valid Profile approved");
            int n= appUserService.updateApprovedAtToken(token);
            request_profile.add(profile.getKolProfileId());
            return "<h3>Profile Approved</h3>";
        }
    }


//    @PostMapping(path="/profile/approved")
//    public void ProfileApprovedDetails(@RequestBody String kolProfileId){
//        log.info("In approved controller kolProfileId-> {} ",kolProfileId);
//        request_profile.add(kolProfileId);
//
//    }

    public List<String> re(){
        List<String> messages = request_profile;
        for(String s:messages){
            RequestProfile profile = appUserService.getRequestProfile(s);
            LocalDateTime approved_time = profile.getApprovedAt();
            if(approved_time.plusHours(12).isBefore(LocalDateTime.now())){
                messages.remove(s);
                appUserService.deleteRequestProfileByKOLID(s);
                return messages;
            }
        }
        return messages;
    }
    @GetMapping(path="/sse")
    public Flux<ServerSentEvent<List<String>>> getAllRequestProfile() {
//        for(String s:request_profile){
//            RequestProfile profile = appUserService.getRequestProfile(s);
//            LocalDateTime approved_time = profile.getApprovedAt();
//            if(approved_time.plusMinutes(1).isBefore(LocalDateTime.now())){
//                request_profile.remove(profile.getKolProfileId());
//            }
//        }
            return Flux.interval(Duration.ofSeconds(1)).map(sequence -> ServerSentEvent.<List<String>>builder()
                    .id(String.valueOf(sequence)).event("all-request-profile-event").data(re()).build());
    }

    @PostMapping(path="/profile/request")
    public void RequestProfile(@RequestBody RequestProfile requestProfile){
        log.info("in controller");
        String token = UUID.randomUUID().toString();
        requestProfile.setToken(token);
        appUserService.saveRequestProfile(requestProfile);
        // TO DO Send mail

        AppUser appUser = appUserService.getAppUser(requestProfile.getUsername());
        String username = appUser.getUsername();

        String send_link = link+"/api/v1/approve?token="+token;
        emailSender.send("akshay.a@glocalmind.com", buildEmail(requestProfile.getKolProfileId(), send_link
        ,username
        ));

    }

    @PostMapping(path = "/user/save")
    public ResponseEntity<AppUser>saveUser(
        @RequestBody AppUser appUser
    ){
        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/v1/user/save").toUriString());

        return ResponseEntity.created(uri).body(
            appUserService.saveAppUser(appUser)
        );
    }

    @PostMapping(path = "/role/save")
    public ResponseEntity<Role>saveRole(
        @RequestBody Role role
    ){
        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/v1/role/save").toUriString());

        return ResponseEntity.created(uri).body(
           appUserService.saveRole(role)
        );
    }
    @PostMapping(path = "/role/addtouser")
    public ResponseEntity<?>saveRoleToUser(
        @RequestBody RoleToAppUserform roleToAppUserform
    ){
        appUserService.addRoleToAppUser(roleToAppUserform.getUseremail(), roleToAppUserform.getRolename());
        return ResponseEntity.ok().build();
    }
    @GetMapping(path="/eg")
    public String testing(){
        return "Checking ROLE only ADMIN can view";
    }
    @GetMapping(path = "/refresh/token")
    public void RefreshToken(
        HttpServletRequest request, HttpServletResponse response
    )throws ServletException, IOException {
        String authorizationHeader = request.getHeader(AUTHORIZATION);
        if(authorizationHeader !=null && authorizationHeader.startsWith("Bearer ")){
            try {
                String refresh_token=authorizationHeader.substring("Bearer ".length());
                Algorithm algorithm = Algorithm.HMAC256(secret_key.getBytes());
                JWTVerifier verifier = JWT.require(algorithm).build();
                DecodedJWT decodedJWT = verifier.verify(refresh_token);
                String username = decodedJWT.getSubject();
                AppUser appUser = appUserService.getAppUser(username);
                String access_token = JWT.create()
                .withSubject(appUser.getEmail())
                //2 months -> 87602
                .withExpiresAt(new Date(System.currentTimeMillis()+ 87602L *60*1000))
                .withIssuer(request.getRequestURI().toString())
                .withClaim("roles", appUser.getRoles().stream().
                map(Role::getName).collect(Collectors.toList()))
                .sign(algorithm);
            
            response.setHeader("access_token",access_token);
            response.setHeader("refresh_token", refresh_token);
            Map<String,String> tokens = new HashMap<>();
            tokens.put("access_token",access_token);
            tokens.put("refresh_token",refresh_token);
            response.setContentType(APPLICATION_JSON_VALUE);
            new ObjectMapper().writeValue(response.getOutputStream(), tokens);
                
            } catch (Exception exception) {
                log.error("Error logging in: {}",exception.getMessage());
                response.setHeader("error",exception.getMessage());
                response.setStatus(FORBIDDEN.value());
               // response.sendError(FORBIDDEN.value());
               //  response.setContentType(APPLICATION_JSON_VALUE);
                Map<String,String> error = new HashMap<>();
                error.put("Error-> ",exception.getMessage());
                new ObjectMapper().writeValue(response.getOutputStream(), error);
            }    
        }else{
            throw new RuntimeException("Refresh token is missing");

        }
    }
    private String buildEmail(String kol_id, String link,String username) {
        return "<div style=\"font-family:Helvetica,Arial,sans-serif;font-size:16px;margin:0;color:#0b0c0c\">\n" +
                "\n" +
                "<span style=\"display:none;font-size:1px;color:#fff;max-height:0\"></span>\n" +
                "\n" +
                "  <table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;min-width:100%;width:100%!important\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
                "    <tbody><tr>\n" +
                "      <td width=\"100%\" height=\"53\" bgcolor=\"#0b0c0c\">\n" +
                "        \n" +
                "        <table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;max-width:580px\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\">\n" +
                "          <tbody><tr>\n" +
                "            <td width=\"70\" bgcolor=\"#0b0c0c\" valign=\"middle\">\n" +
                "                <table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">\n" +
                "                  <tbody><tr>\n" +
                "                    <td style=\"padding-left:10px\">\n" +
                "                  \n" +
                "                    </td>\n" +
                "                    <td style=\"font-size:28px;line-height:1.315789474;Margin-top:4px;padding-left:10px\">\n" +
                "                      <span style=\"font-family:Helvetica,Arial,sans-serif;font-weight:700;color:#ffffff;text-decoration:none;vertical-align:top;display:inline-block\">Request for KOL profile</span>\n" +
                "                    </td>\n" +
                "                  </tr>\n" +
                "                </tbody></table>\n" +
                "              </a>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "        </tbody></table>\n" +
                "        \n" +
                "      </td>\n" +
                "    </tr>\n" +
                "  </tbody></table>\n" +
                "  <table role=\"presentation\" class=\"m_-6186904992287805515content\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;max-width:580px;width:100%!important\" width=\"100%\">\n" +
                "    <tbody><tr>\n" +
                "      <td width=\"10\" height=\"10\" valign=\"middle\"></td>\n" +
                "      <td>\n" +
                "        \n" +
                "                <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">\n" +
                "                  <tbody><tr>\n" +
                "                    <td bgcolor=\"#1D70B8\" width=\"100%\" height=\"10\"></td>\n" +
                "                  </tr>\n" +
                "                </tbody></table>\n" +
                "        \n" +
                "      </td>\n" +
                "      <td width=\"10\" valign=\"middle\" height=\"10\"></td>\n" +
                "    </tr>\n" +
                "  </tbody></table>\n" +
                "\n" +
                "\n" +
                "\n" +
                "  <table role=\"presentation\" class=\"m_-6186904992287805515content\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;max-width:580px;width:100%!important\" width=\"100%\">\n" +
                "    <tbody><tr>\n" +
                "      <td height=\"30\"><br></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "      <td width=\"10\" valign=\"middle\"><br></td>\n" +
                "      <td style=\"font-family:Helvetica,Arial,sans-serif;font-size:19px;line-height:1.315789474;max-width:560px\">\n" +
                "        \n" +
//                            "<p> HI </p>\n"+
                "            <p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\"> Requested By: "+username+ "\n"+"<br/>"+" Kol Profile Id :" + kol_id + ",</p><p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\">Please click on the below link to approve Kol profile: </p><blockquote style=\"Margin:0 0 20px 0;border-left:10px solid #b1b4b6;padding:15px 0 0.1px 15px;font-size:19px;line-height:25px\"><p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\"> <a href=\"" + link + "\">Approve Now</a> </p><p>Link is valid for 12 hours</p></blockquote>\n " +
                "        \n" +
                "      </td>\n" +
                "      <td width=\"10\" valign=\"middle\"><br></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "      <td height=\"30\"><br></td>\n" +
                "    </tr>\n" +
                "  </tbody></table><div class=\"yj6qo\"></div><div class=\"adL\">\n" +
                "\n" +
                "</div></div>";
    }

   
}
@Data
class RoleToAppUserform{
    private String useremail;
    private String rolename;
}


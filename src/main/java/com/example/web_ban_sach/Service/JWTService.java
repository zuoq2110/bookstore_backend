package com.example.web_ban_sach.Service;

import com.example.web_ban_sach.entity.NguoiDung;
import com.example.web_ban_sach.entity.Quyen;
import com.example.web_ban_sach.util.UserSecurityService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class JWTService {
    @Autowired
    private UserSecurityService userSecurityService;
    
    @Value("${jwt.secret}")
    private String SECRET;
    
    @Value("${jwt.expiration}")
    private long ACCESS_TOKEN_VALIDITY;
    
    @Value("${jwt.refresh-expiration}")
    private long REFRESH_TOKEN_VALIDITY;

    public String generateToken(String tenDangNhap){
        Map<String, Object> claims = new HashMap<>();
        NguoiDung nguoiDung = userSecurityService.findByTenDangNhap(tenDangNhap);
        boolean isAdmin = false;
        boolean isStaff = false;
        boolean isUser = false;
        if(nguoiDung!=null && nguoiDung.getDanhSachQuyen().size()>0){
            List<Quyen> list = nguoiDung.getDanhSachQuyen();
            for(Quyen q: list){
                if(q.getTenQuyen().equals("ADMIN")){
                    isAdmin = true;
                }
                if(q.getTenQuyen().equals("STAFF")){
                    isStaff = true;
                }
                if(q.getTenQuyen().equals("USER")){
                    isUser = true;
                }
            }
        }
        claims.put("id", nguoiDung.getMaNguoiDung());
        claims.put("isAdmin", isAdmin);
        claims.put("isStaff", isStaff);
        claims.put("isUser", isUser);
        claims.put("enabled", nguoiDung.isDaKichHoat());
        return createToken(claims, tenDangNhap, ACCESS_TOKEN_VALIDITY);
    }
    
    public String generateRefreshToken(String tenDangNhap){
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return createToken(claims, tenDangNhap, REFRESH_TOKEN_VALIDITY);
    }

    private String createToken(Map<String, Object> claims, String tenDangNhap, long validity){
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(tenDangNhap)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + validity))
                .signWith(SignatureAlgorithm.HS256, getSignedKey())
                .compact();
    }
    
    public boolean isRefreshToken(String token){
        Claims claims = extractAllClaims(token);
        return "refresh".equals(claims.get("type"));
    }

    private Key getSignedKey(){
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes); // This will work with 512+ bit key
    }

    public Claims extractAllClaims(String token){
        return Jwts.parser().setSigningKey(getSignedKey()).parseClaimsJws(token).getBody();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsTFunction){
        Claims claims = extractAllClaims(token);
        return claimsTFunction.apply(claims);
    }

    public Date extractExpiration(String token){
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractUsername(String token){
        return extractClaim(token, Claims::getSubject);
    }

    private Boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, UserDetails userDetails){
        final String tenDangNhap = extractUsername(token);
        return (tenDangNhap.equals(userDetails.getUsername())&&!isTokenExpired(token));
    }

}

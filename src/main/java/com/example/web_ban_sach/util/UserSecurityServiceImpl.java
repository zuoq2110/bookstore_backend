package com.example.web_ban_sach.util;

import com.example.web_ban_sach.dao.NguoiDungRepository;
import com.example.web_ban_sach.dao.QuyenRepository;
import com.example.web_ban_sach.entity.NguoiDung;
import com.example.web_ban_sach.entity.Quyen;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;
@Service
public class UserSecurityServiceImpl implements UserSecurityService{
    @Autowired
    NguoiDungRepository nguoiDungRepository;
    @Autowired
    QuyenRepository quyenRepository;
    @Override
    public NguoiDung findByTenDangNhap(String tenDangNhap) {
        return nguoiDungRepository.findByTenDangNhap(tenDangNhap);
    }
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        NguoiDung nguoiDung = findByTenDangNhap(username);
        if(nguoiDung==null){
            throw new UsernameNotFoundException("Tài khoản không tồn tại");
        }
        return new User(nguoiDung.getTenDangNhap(), nguoiDung.getMatKhau(),rolesToAuthorities(nguoiDung.getDanhSachQuyen()) ) ;
    }

    private Collection<? extends GrantedAuthority> rolesToAuthorities(Collection<Quyen> quyens){
        return quyens.stream().map(quyen -> new SimpleGrantedAuthority(quyen.getTenQuyen())).collect(Collectors.toList());
    }
}

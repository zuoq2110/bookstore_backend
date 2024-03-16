package com.example.web_ban_sach.config;

import com.example.web_ban_sach.entity.NguoiDung;
import com.example.web_ban_sach.entity.TheLoai;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
@Configuration
public class MethodRestConfig implements RepositoryRestConfigurer {
    private String url = "http://localhost:3000";

    @Autowired
    private EntityManager entityManager;
    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {


        // expose ids
        // Cho phép trả về id
        config.exposeIdsFor(entityManager.getMetamodel().getEntities().stream().map(Type::getJavaType).toArray(Class[]::new));
        // config.exposeIdsFor(TheLoai.class);

        // Chặn các methods
        HttpMethod[] chanCacPhuongThuc ={
                HttpMethod.POST,
                HttpMethod.PUT,
                HttpMethod.PATCH,
                HttpMethod.DELETE,
        };
        disableHttpMethods(TheLoai.class, config, chanCacPhuongThuc);

        // Chặn các method DELETE
        HttpMethod[] phuongThucDelete = {
                HttpMethod.DELETE
        };
        disableHttpMethods(NguoiDung.class, config,phuongThucDelete );
        // CORS configuration
        cors.addMapping("/**")
                .allowedOrigins(url)
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }



    private void disableHttpMethods(Class c,
                                    RepositoryRestConfiguration config,
                                    HttpMethod[] methods){
        config.getExposureConfiguration()
                .forDomainType(c)
                .withItemExposure((metdata, httpMethods) -> httpMethods.disable(methods))
                .withCollectionExposure((metdata, httpMethods) -> httpMethods.disable(methods));
    }
}

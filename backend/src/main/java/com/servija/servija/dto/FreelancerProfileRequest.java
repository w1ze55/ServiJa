package com.servija.servija.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreelancerProfileRequest {
    
    @Size(min = 3, max = 255, message = "Nome deve ter entre 3 e 255 caracteres")
    private String name;
    
    @Size(max = 100, message = "Profissão deve ter no máximo 100 caracteres")
    private String profession;
    
    @Size(max = 50, message = "Categoria deve ter no máximo 50 caracteres")
    private String category;

    private Long categoryId;

    private Long subcategoryId;

    @Size(max = 160, message = "Subcategoria deve ter no maximo 160 caracteres")
    private String subcategory;
    
    @Size(max = 1000, message = "Bio deve ter no máximo 1000 caracteres")
    private String bio;
    
    private String avatarUrl;
    
    @Size(max = 20, message = "Tipo de serviço deve ter no máximo 20 caracteres")
    private String serviceType;
    
    @Size(max = 20, message = "Máximo de 20 cidades permitidas")
    private List<String> cities;
    
    private List<ServiceCityResponse> serviceCities;
    
    private SocialLinks socialLinks;
    
    private List<ProfessionalLinkResponse> professionalLinks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SocialLinks {
        @Size(max = 255, message = "URL do website deve ter no máximo 255 caracteres")
        private String website;
        
        @Size(max = 255, message = "URL do LinkedIn deve ter no máximo 255 caracteres")
        private String linkedin;
        
        @Size(max = 255, message = "URL do Instagram deve ter no máximo 255 caracteres")
        private String instagram;
        
        @Size(max = 255, message = "URL do portfólio deve ter no máximo 255 caracteres")
        private String portfolio;
    }
}


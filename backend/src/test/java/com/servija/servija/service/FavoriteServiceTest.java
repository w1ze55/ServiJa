package com.servija.servija.service;

import com.servija.servija.dto.FavoriteRequest;
import com.servija.servija.dto.FavoriteResponse;
import com.servija.servija.model.Address;
import com.servija.servija.model.Favorite;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.User;
import com.servija.servija.repository.AddressRepository;
import com.servija.servija.repository.FavoriteRepository;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.ServiceRepository;
import com.servija.servija.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private FreelancerRepository freelancerRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServiceService serviceService;

    @Mock
    private AuditLogger auditLogger;

    private FavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        favoriteService = new FavoriteService(
                favoriteRepository,
                serviceRepository,
                freelancerRepository,
                addressRepository,
                userRepository,
                serviceService,
                auditLogger);
    }

    @Test
    void getUserFavoritesUsesDefaultAddressForFreelancerLocation() {
        User user = buildUser(10L, User.UserType.CONTRACTOR);
        Freelancer freelancer = buildFreelancer(20L, "Ana Freelancer");
        Favorite favorite = Favorite.builder()
                .id(1L)
                .user(user)
                .freelancer(freelancer)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(favoriteRepository.findByUser(user)).thenReturn(List.of(favorite));
        when(addressRepository.findByUserIdInOrderByUserIdAscIsDefaultDescCreatedAtDesc(List.of(freelancer.getId())))
                .thenReturn(List.of(buildAddress(freelancer, "Sao Jose", "SC", true)));

        List<FavoriteResponse> response = favoriteService.getUserFavorites(user.getId());

        assertEquals(1, response.size());
        assertEquals("Sao Jose, SC", response.get(0).getFreelancer().getLocation());
    }

    @Test
    void addFavoriteFallsBackToFreelancerLocationWhenNoAddressExists() {
        User user = buildUser(10L, User.UserType.CONTRACTOR);
        Freelancer freelancer = buildFreelancer(20L, "Ana Freelancer");
        freelancer.setLocation("Florianopolis, SC");
        Favorite savedFavorite = Favorite.builder()
                .id(2L)
                .user(user)
                .freelancer(freelancer)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(favoriteRepository.findByUserIdAndFreelancerId(user.getId(), freelancer.getId())).thenReturn(Optional.empty());
        when(freelancerRepository.findById(freelancer.getId())).thenReturn(Optional.of(freelancer));
        when(favoriteRepository.save(any(Favorite.class))).thenReturn(savedFavorite);
        when(addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(freelancer.getId())).thenReturn(List.of());

        FavoriteResponse response = favoriteService.addFavorite(
                user.getId(),
                FavoriteRequest.builder().freelancerId(freelancer.getId()).build());

        assertEquals("Florianopolis, SC", response.getFreelancer().getLocation());
    }

    @Test
    void addFavoriteReturnsNullLocationWhenNoAddressOrFallbackExists() {
        User user = buildUser(10L, User.UserType.CONTRACTOR);
        Freelancer freelancer = buildFreelancer(20L, "Ana Freelancer");
        Favorite savedFavorite = Favorite.builder()
                .id(3L)
                .user(user)
                .freelancer(freelancer)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(favoriteRepository.findByUserIdAndFreelancerId(user.getId(), freelancer.getId())).thenReturn(Optional.empty());
        when(freelancerRepository.findById(freelancer.getId())).thenReturn(Optional.of(freelancer));
        when(favoriteRepository.save(any(Favorite.class))).thenReturn(savedFavorite);
        when(addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(freelancer.getId())).thenReturn(List.of());

        FavoriteResponse response = favoriteService.addFavorite(
                user.getId(),
                FavoriteRequest.builder().freelancerId(freelancer.getId()).build());

        assertNull(response.getFreelancer().getLocation());
    }

    private User buildUser(Long id, User.UserType type) {
        User user = new User();
        user.setId(id);
        user.setName("Usuario");
        user.setEmail("user@test.com");
        user.setCpf("12345678901");
        user.setPhone("11999999999");
        user.setUserType(type);
        return user;
    }

    private Freelancer buildFreelancer(Long id, String name) {
        Freelancer freelancer = new Freelancer();
        freelancer.setId(id);
        freelancer.setName(name);
        freelancer.setEmail("freelancer@test.com");
        freelancer.setCpf("12345678902");
        freelancer.setPhone("11999999998");
        freelancer.setUserType(User.UserType.FREELANCER);
        freelancer.setProfessionalTitle("Designer");
        freelancer.setCategory("Design");
        freelancer.setAvailable(true);
        return freelancer;
    }

    private Address buildAddress(User user, String city, String state, boolean isDefault) {
        Address address = new Address();
        address.setId(100L);
        address.setUser(user);
        address.setCity(city);
        address.setState(state);
        address.setIsDefault(isDefault);
        return address;
    }
}

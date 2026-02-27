package com.servija.servija.controller;

import com.servija.servija.dto.FavoriteRequest;
import com.servija.servija.dto.FavoriteResponse;
import com.servija.servija.dto.MessageResponse;
import com.servija.servija.model.User;
import com.servija.servija.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorites", description = "Endpoints for managing user favorites (services and freelancers)")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping
    @Operation(summary = "Add a service or freelancer to favorites")
    public ResponseEntity<FavoriteResponse> addFavorite(
            @AuthenticationPrincipal User user,
            @RequestBody FavoriteRequest request) {
        return ResponseEntity.ok(favoriteService.addFavorite(user.getId(), request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a favorite by its ID")
    public ResponseEntity<MessageResponse> removeFavorite(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        favoriteService.removeFavorite(user.getId(), id);
        return ResponseEntity.ok(new MessageResponse("Favorite removed successfully"));
    }

    @GetMapping
    @Operation(summary = "List all favorites for the current user")
    public ResponseEntity<List<FavoriteResponse>> getUserFavorites(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(favoriteService.getUserFavorites(user.getId()));
    }

    @GetMapping("/check")
    @Operation(summary = "Check if a specific service or freelancer is favorited")
    public ResponseEntity<Boolean> checkFavorite(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) Long freelancerId) {
        return ResponseEntity.ok(favoriteService.isFavorited(user.getId(), serviceId, freelancerId));
    }
}

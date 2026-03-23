package com.kilgore.fooddeliveryapp.controller;

import com.kilgore.fooddeliveryapp.dto.request.GroupDealParticipationRequest;
import com.kilgore.fooddeliveryapp.dto.request.GroupDealRequest;
import com.kilgore.fooddeliveryapp.dto.response.GroupDealParticipationResponse;
import com.kilgore.fooddeliveryapp.dto.response.GroupDealResponse;
import com.kilgore.fooddeliveryapp.service.GroupDealService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants/{restaurantId}/group-deals")
public class GroupDealController {

    private final GroupDealService groupDealService;

    public GroupDealController(GroupDealService groupDealService) {
        this.groupDealService = groupDealService;
    }

    //----------------------------------------------FOR RESTAURANT-OWNER------------------------------------------------

    @GetMapping
    public List<GroupDealResponse> getDealsByRestaurant(@PathVariable Long restaurantId) {
        return groupDealService.getDealsForRestaurant(restaurantId);
    }
    
    @PostMapping
    @PreAuthorize("hasAnyAuthority('RESTAURANT_OWNER')")
    public GroupDealResponse createGroupDeal(@PathVariable Long restaurantId,
                                             @RequestBody GroupDealRequest request) {
        return groupDealService.createGroupDeal(restaurantId, request);
    }

    @DeleteMapping("/{dealId}")
    @PreAuthorize("hasAnyAuthority('RESTAURANT_OWNER')")
    public String deleteGroupDeal(@PathVariable Long dealId, @PathVariable Long restaurantId) {
        return groupDealService.deleteGroupDeal(restaurantId, dealId);
    }


    @GetMapping("/{dealId}/participations")
    @PreAuthorize("hasAnyAuthority('RESTAURANT_OWNER')")
    public List<GroupDealParticipationResponse> getParticipationsByDeal(@PathVariable Long restaurantId,
                                                                        @PathVariable Long dealId) {
        return groupDealService.getParticipationsByDeal(restaurantId, dealId);
    }

    //----------------------------------------------------CUSTOMERS-----------------------------------------------------


    @GetMapping("/{dealId}")
    public GroupDealResponse getDeal(@PathVariable Long restaurantId,
                                                @PathVariable Long dealId) {
        return groupDealService.getDeal(restaurantId, dealId);
    }

    @GetMapping("/{dealId}/participate")
    public List<GroupDealParticipationResponse> getParticipationsByUser(@PathVariable Long restaurantId,
                                                                  @PathVariable Long dealId) {
        return groupDealService.getParticipationsByUser(restaurantId,dealId);
    }

    @PostMapping("{dealId}/participate")
    public GroupDealParticipationResponse participateInGroupDeal(@PathVariable Long restaurantId,
                                                                 @PathVariable Long dealId,
                                                                 @RequestBody GroupDealParticipationRequest request) {
        return groupDealService.participateInGroupDeal(restaurantId, dealId, request);
    }

    @PutMapping("{dealId}/participate/{participationId}")
    public String withdrawFromGroupDeal(@PathVariable Long restaurantId,
                                        @PathVariable Long dealId,
                                        @PathVariable  Long participationId) {
        return groupDealService.withdrawFromGroupDeal(restaurantId, dealId, participationId);
    }
}

package com.deliveryflow.mapper;

import com.deliveryflow.api.dto.response.OrderResponse;
import com.deliveryflow.api.dto.response.StatusHistoryResponse;
import com.deliveryflow.domain.entity.Order;
import com.deliveryflow.domain.entity.OrderStatusHistory;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {

    OrderResponse toResponse(Order order);

    List<StatusHistoryResponse> toHistoryResponseList(List<OrderStatusHistory> history);
}

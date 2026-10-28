package com.deliveryflow.mapper;

import com.deliveryflow.api.dto.response.UserResponse;
import com.deliveryflow.domain.entity.User;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

  UserResponse toResponse(User user);

  List<UserResponse> toResponseList(List<User> users);
}

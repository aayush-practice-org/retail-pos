package io.aygh.customer.service.query;

import io.aygh.customer.dto.response.CustomerResponse;
import io.aygh.shared.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomerQueryService {

    CustomerResponse findById(Long id);

    CustomerResponse findByPhone(String phone);

    PagedResponse<CustomerResponse> findAll(String search, Pageable pageable);

    List<CustomerResponse> findAllForSelection();
}

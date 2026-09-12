package io.aygh.customer.service.command;

import io.aygh.customer.dto.request.CustomerRequest;
import io.aygh.customer.dto.request.CustomerSettlementRequest;
import io.aygh.customer.dto.response.CustomerResponse;
import io.aygh.customer.dto.response.CustomerSettlementResponse;

public interface CustomerCommandService {

    CustomerResponse create(CustomerRequest request);

    CustomerResponse update(Long id, CustomerRequest request);

    void delete(Long id);

    CustomerSettlementResponse settle(Long id, CustomerSettlementRequest request);
}

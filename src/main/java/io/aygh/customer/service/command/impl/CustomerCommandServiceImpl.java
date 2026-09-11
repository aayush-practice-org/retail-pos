package io.aygh.customer.service.command.impl;

import io.aygh.customer.dto.request.CustomerRequest;
import io.aygh.customer.dto.response.CustomerResponse;
import io.aygh.customer.entity.Customer;
import io.aygh.customer.helper.CustomerResolver;
import io.aygh.customer.helper.CustomerValidation;
import io.aygh.customer.mapper.CustomerMapper;
import io.aygh.customer.repository.CustomerRepository;
import io.aygh.customer.service.command.CustomerCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerCommandServiceImpl implements CustomerCommandService {

    private final CustomerRepository customerRepository;
    private final CustomerResolver resolver;
    private final CustomerValidation validation;
    private final CustomerMapper customerMapper;

    @Override
    public CustomerResponse create(CustomerRequest request) {
        validation.requirePhoneAvailable(request.phone(), null);
        validation.requirePanAvailable(request.panNumber(), null);

        Customer customer = customerMapper.toEntity(request);
        if (request.active() != null) {
            customer.setActive(request.active());
        }
        Customer saved = customerRepository.save(customer);
        log.info("Registered customer '{}' ({})", saved.getName(), saved.getPhone());
        return customerMapper.toResponse(saved);
    }

    @Override
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = resolver.customer(id);

        validation.requirePhoneAvailable(request.phone(), id);
        validation.requirePanAvailable(request.panNumber(), id);

        customerMapper.applyUpdate(request, customer);
        if (request.active() != null) {
            customer.setActive(request.active());
        }
        Customer saved = customerRepository.save(customer);
        log.info("Updated customer '{}' ({})", saved.getName(), saved.getPhone());
        return customerMapper.toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        Customer customer = resolver.customer(id);
        customerRepository.delete(customer);
        log.info("Removed customer '{}' ({})", customer.getName(), customer.getPhone());
    }
}

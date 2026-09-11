package io.aygh.customer.service.query.impl;

import io.aygh.customer.dto.response.CustomerResponse;
import io.aygh.customer.entity.Customer;
import io.aygh.customer.helper.CustomerResolver;
import io.aygh.customer.mapper.CustomerMapper;
import io.aygh.customer.repository.CustomerRepository;
import io.aygh.customer.service.query.CustomerQueryService;
import io.aygh.exception.ResourceNotFoundException;
import io.aygh.shared.response.PagedResponse;
import io.aygh.shared.response.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerQueryServiceImpl implements CustomerQueryService {

    private final CustomerRepository customerRepository;
    private final CustomerResolver resolver;
    private final CustomerMapper customerMapper;

    @Override
    public CustomerResponse findById(Long id) {
        return customerMapper.toResponse(resolver.customer(id));
    }

    @Override
    public CustomerResponse findByPhone(String phone) {
        return customerRepository.findByPhone(phone)
                .map(customerMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "phone", phone));
    }

    @Override
    public PagedResponse<CustomerResponse> findAll(String search, Pageable pageable) {
        Specification<Customer> spec = matches(search);
        Page<Customer> page = spec == null
                ? customerRepository.findAll(pageable)
                : customerRepository.findAll(spec, pageable);

        return PaginationUtils.toPagedResponse(page, page.map(customerMapper::toResponse).getContent());
    }

    @Override
    public List<CustomerResponse> findAllForSelection() {
        return customerMapper.toResponses(customerRepository.findAll(Sort.by("name")));
    }

    private static Specification<Customer> matches(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("phone")), pattern),
                cb.like(cb.lower(root.get("panNumber")), pattern));
    }
}

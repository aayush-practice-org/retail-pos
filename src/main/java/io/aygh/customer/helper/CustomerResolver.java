package io.aygh.customer.helper;

import io.aygh.customer.entity.Customer;
import io.aygh.customer.repository.CustomerRepository;
import io.aygh.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Loading customer rows by id, with a 404 ResourceNotFoundException instead of an empty Optional.
 */
@Component
@RequiredArgsConstructor
public class CustomerResolver {

    private final CustomerRepository customerRepository;

    public Customer customer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
    }

    public void requireCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Customer", "id", id);
        }
    }
}

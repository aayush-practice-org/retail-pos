package io.aygh.customer.helper;

import io.aygh.customer.repository.CustomerRepository;
import io.aygh.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerValidation {

    private final CustomerRepository customerRepository;

    public void requirePhoneAvailable(String phone, Long excludingId) {
        if (phone == null || phone.isBlank()) {
            return;
        }
        boolean taken = excludingId == null
                ? customerRepository.existsByPhone(phone.trim())
                : customerRepository.existsByPhoneAndIdNot(phone.trim(), excludingId);
        if (taken) {
            throw new BusinessException("A customer with phone number '" + phone + "' already exists");
        }
    }

    public void requirePanAvailable(String panNumber, Long excludingId) {
        if (panNumber == null || panNumber.isBlank()) {
            return;
        }
        boolean taken = excludingId == null
                ? customerRepository.existsByPanNumberIgnoreCase(panNumber.trim())
                : customerRepository.existsByPanNumberIgnoreCaseAndIdNot(panNumber.trim(), excludingId);
        if (taken) {
            throw new BusinessException("PAN '" + panNumber + "' is already registered to another customer");
        }
    }
}

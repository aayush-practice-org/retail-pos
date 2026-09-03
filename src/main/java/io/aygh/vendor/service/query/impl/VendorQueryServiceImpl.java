package io.aygh.vendor.service.query.impl;

import io.aygh.shared.response.PagedResponse;
import io.aygh.shared.response.PaginationUtils;
import io.aygh.vendor.dto.response.VendorResponse;
import io.aygh.vendor.entity.Vendor;
import io.aygh.vendor.helper.VendorResolver;
import io.aygh.vendor.mapper.VendorMapper;
import io.aygh.vendor.repository.VendorRepository;
import io.aygh.vendor.service.query.VendorQueryService;
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
public class VendorQueryServiceImpl implements VendorQueryService {

    private final VendorRepository vendorRepository;
    private final VendorResolver resolver;
    private final VendorMapper vendorMapper;

    @Override
    public VendorResponse findById(Long id) {
        return vendorMapper.toResponse(resolver.vendor(id));
    }

    @Override
    public PagedResponse<VendorResponse> findAll(String search, Pageable pageable) {
        Specification<Vendor> spec = matches(search);
        Page<Vendor> page = spec == null
                ? vendorRepository.findAll(pageable)
                : vendorRepository.findAll(spec, pageable);

        return PaginationUtils.toPagedResponse(page, page.map(vendorMapper::toResponse).getContent());
    }

    @Override
    public List<VendorResponse> findAllForSelection() {
        return vendorMapper.toResponses(vendorRepository.findAll(Sort.by("name")));
    }

    /**
     * Contact number and PAN are matched as well as the name: a buyer looking a
     * vendor up usually has the bill in hand, and the bill carries those.
     */
    private static Specification<Vendor> matches(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("contactNumber")), pattern),
                cb.like(cb.lower(root.get("panNumber")), pattern));
    }
}

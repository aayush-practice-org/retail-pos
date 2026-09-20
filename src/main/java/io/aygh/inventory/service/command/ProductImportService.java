package io.aygh.inventory.service.command;

import io.aygh.inventory.dto.response.ProductImportResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ProductImportService {

    /**
     * Reads a catalogue out of a spreadsheet.
     * <p>
     * All or nothing: the whole file is checked first, and a single bad row
     * means none of it is written. A half-imported catalogue is worse than no
     * import, because the failed rows are indistinguishable from the ones
     * nobody has got to yet.
     *
     * @param dryRun check the file and report, writing nothing
     */
    ProductImportResponse importFrom(MultipartFile file, boolean dryRun);
}

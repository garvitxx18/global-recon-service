package global.recon.service.repository;

import global.recon.service.model.Source;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SourceRepository extends JpaRepository<Source, String> {

    List<Source> findByEnabledTrueOrderByNameAsc();
}

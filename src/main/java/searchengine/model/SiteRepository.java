package searchengine.model;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface SiteRepository extends CrudRepository<Site, Long> {
    @Query("DELETE s.url FROM site s")
    public void delete(@Param("url") String url);
}

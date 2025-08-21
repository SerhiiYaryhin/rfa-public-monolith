package media.toloka.rfa.banner.repositore;

import media.toloka.rfa.banner.model.Banner;
import media.toloka.rfa.radio.model.Clientdetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, String> {
//    List<Banner> findByClientdetail(Clientdetail cd);
//    List<Banner> findByClientdetail(String uuid);
//    Banner getByUuid(String ChanelUuid);
//    List<Banner> getByApproveOrderByIdDesc();
    void deleteById(String uuid);
}

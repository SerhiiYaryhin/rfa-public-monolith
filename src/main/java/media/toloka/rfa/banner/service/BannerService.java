package media.toloka.rfa.banner.service;

import media.toloka.rfa.banner.model.Banner;
import media.toloka.rfa.banner.repositore.BannerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BannerService {

//    private final BannerRepository bannerRepository;
    @Autowired
    private  BannerRepository bannerRepository;

    public Page<Banner>  BannerGetPage(int page, int size) {
        return bannerRepository.findAll(PageRequest.of(page, size, Sort.by("id").descending()));
    }

    public List<Banner> BannerGetAll() {
        return bannerRepository.findAll();
    }

    public Banner BannerGetByUUID(String uuid) {
        Optional<Banner> bannerOpt = bannerRepository.findById(uuid);
        if (bannerOpt.isEmpty()) {
            return null;
        }
        return bannerOpt.get();
    }

    public void BannerSave(Banner banner) {
        bannerRepository.save(banner);
    }

    public void BannerDelete(String uuid ) {
        bannerRepository.deleteById(uuid);
    }

}

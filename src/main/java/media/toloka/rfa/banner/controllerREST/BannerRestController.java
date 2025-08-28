package media.toloka.rfa.banner.controllerREST;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import media.toloka.rfa.banner.model.Banner;
import media.toloka.rfa.banner.repositore.BannerRepository;
import media.toloka.rfa.banner.model.enumerate.EBannerType;
import media.toloka.rfa.radio.store.model.Store;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/2.0/banners")
@RequiredArgsConstructor
public class BannerRestController {

    private final BannerRepository bannerRepository;
    private final Random random = new Random();

    // Допоміжний клас для десеріалізації запиту
    @Data
    public static class BannerRequest {
        private EBannerType type;
        private int count;
    }


    /**
     * Повертає список схвалених банерів заданого типу, або всі схвалені, якщо тип не вказано.
     */
    @GetMapping
    public List<Banner> getApprovedBanners(@RequestParam(required = false) EBannerType type) {
        List<Banner> allApprovedBanners = bannerRepository.findAll().stream()
                .filter(banner -> Boolean.TRUE.equals(banner.getApprove()))
                .collect(Collectors.toList());

        if (type != null) {
            return allApprovedBanners.stream()
                    .filter(banner -> type.equals(banner.getBannertype()))
                    .collect(Collectors.toList());
        }

        // Повертаємо всі схвалені банери, якщо тип не вказано
        return allApprovedBanners;
    }

    /**
     * Повертає вказану кількість випадкових схвалених банерів.
     */
    @GetMapping("/random")
    public List<Banner> getRandomApprovedBanners(@RequestParam(defaultValue = "1") int count) {
        List<Banner> allApprovedBanners = bannerRepository.findAll().stream()
                .filter(banner -> Boolean.TRUE.equals(banner.getApprove()))
                .collect(Collectors.toList());

        Collections.shuffle(allApprovedBanners); // Перемішуємо список

        // Повертаємо перші 'count' елементів
        return allApprovedBanners.stream()
                .limit(count)
                .collect(Collectors.toList());
    }

    /**
     * Збільшує лічильник переглядів банера за його UUID.
     */
    @PostMapping("/{uuid}/view")
    public ResponseEntity<Void> incrementView(@PathVariable String uuid) {
        Optional<Banner> bannerOpt = bannerRepository.findById(uuid);
        if (bannerOpt.isPresent()) {
            Banner banner = bannerOpt.get();
            banner.setViews(banner.getViews() + 1);
            bannerRepository.save(banner);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Збільшує лічильник переходів (кліків) банера за його UUID.
     */
    @PostMapping("/{uuid}/transition")
    public ResponseEntity<Void> incrementTransition(@PathVariable String uuid) {
        Optional<Banner> bannerOpt = bannerRepository.findById(uuid);
        if (bannerOpt.isPresent()) {
            Banner banner = bannerOpt.get();
            banner.setTransition(banner.getTransition() + 1);
            bannerRepository.save(banner);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Повертає масив об'єктів-банерів.
     * Параметр 'requests' може мати вигляд: [{"type": "TEXT", "count": 2}, {"type": "IMAGE", "count": 1}]
     */
    @PostMapping("/load")
    public List<Banner> loadBanners(@RequestBody List<BannerRequest> requests) {
        List<Banner> allApprovedBanners = bannerRepository.findAll().stream()
                .filter(banner -> Boolean.TRUE.equals(banner.getApprove()))
                .collect(Collectors.toList());

        List<Banner> resultBanners = new ArrayList<>();

        for (BannerRequest request : requests) {
            List<Banner> filteredBanners = allApprovedBanners.stream()
                    .filter(banner -> request.getType().equals(banner.getBannertype()))
                    .collect(Collectors.toList());

            // Перемішуємо, щоб отримати випадкові
            Collections.shuffle(filteredBanners, random);

            // Додаємо потрібну кількість банерів
            resultBanners.addAll(
                    filteredBanners.stream()
                            .limit(request.getCount())
                            .collect(Collectors.toList())
            );
        }

        resultBanners.forEach(banner -> banner.setClientdetail(null));
        resultBanners.forEach(banner -> banner.setStore(null));
//        for (Banner ban : resultBanners) {
//            ban.setUuidmedia(ban.getStore().getUuid());
//        }
//        resultBanners.forEach(banner -> banner.setStore(banner.getStore().getUuid()));

        return resultBanners;
    }


}
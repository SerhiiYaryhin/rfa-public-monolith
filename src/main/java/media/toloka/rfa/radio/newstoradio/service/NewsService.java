package media.toloka.rfa.radio.newstoradio.service;

import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.newstoradio.model.News;
import media.toloka.rfa.radio.newstoradio.repository.NewsRepositore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class NewsService {

    @Autowired
    private NewsRepositore newsRepositore;

    public Page GetNewsPageByClientDetail(int pageNumber, int pageCount, Clientdetail cd) {
        Pageable NewsPage = PageRequest.of(pageNumber, pageCount);
        return newsRepositore.findByClientdetailOrderByCreatedateDesc(NewsPage,cd);
    }

    public News GetByUUID(String uuidnews) {
        return newsRepositore.getByUuid(uuidnews);
    }
}
